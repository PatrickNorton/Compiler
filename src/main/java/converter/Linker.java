package main.java.converter;

import main.java.parser.BaseClassNode;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContextDefinitionNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DefinitionNode;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.LineInfo;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.TopLevelNode;
import main.java.parser.TopNode;
import main.java.parser.TypeNode;
import main.java.parser.TypedefStatementNode;
import main.java.parser.UnionDefinitionNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The class to link the {@link TopNode} representing a file.
 * <p>
 *     Linking consists of multiple actions. If the file does not export
 *     anything, then it is not a module. If it is not a module, arbitrary code
 *     is allowed at the top level and all types must be referenced after they
 *     are defined, and as such linking is not run. Otherwise, only {@link
 *     DefinitionNode definitions} or {@link ImportExportNode import/export
 *     statements} are allowed. If this is the case, linking is run. To link
 *     a file, use the {@link #link link method}. To see more of what happens
 *     during linking, see that method's documentation
 * </p>
 * @author Patrick Norton
 * @see #link
 */
public final class Linker {
    //  TODO? Should this link regardless?
    //        How should that interact with circular references?
    //        What should be allowed/not allowed?
    private final CompilerInfo info;
    private final Map<String, Pair<String, LineInfo>> exports;
    private final Map<String, TypeObject> globals;
    private final Map<String, Integer> constants;

    public Linker(CompilerInfo info) {
        this.info = info;
        this.exports = new HashMap<>();
        this.globals = new HashMap<>();
        this.constants = new HashMap<>();
    }

    public Map<String, Pair<String, LineInfo>> getExports() {
        return exports;
    }

    public Map<String, TypeObject> getGlobals() {
        return globals;
    }

    public Map<String, Integer> getConstants() {
        return constants;
    }

    /**
     * Link the TopNode passed.
     * <p>
     *     This assumes that the {@link CompilerInfo} associated with the
     *     Linker has not already been linked.
     * </p>
     * <p>
     *     If the module is not a module, i.e. it has (has no {@link
     *     ImportExportNode 'export' statements}), no pre-declarations will
     *     be parsed, in accordance with the language spec. Otherwise, this
     *     will parse each top-level statement, put the names into its globals
     *     map, check for double-definitions, and check to ensure all statements
     *     are legal for an exporting module.
     * </p>
     * @param node The node to link using
     * @return Itself
     */
    public Linker link(TopNode node) {
        info.loadDependents();
        if (!isModule(node)) {
            return this;
        }
        var importedTypes = info.importHandler().importedTypes();
        info.addPredeclaredTypes(importedTypes);
        info.addStackFrame();  // Needed for variable-declaration constant-folding
        // Filters out auto interfaces, which are registered earlier
        var importHandler = info.importHandler();
        for (var stmt : node) {
            if (!(stmt instanceof TopLevelNode)) {
                throw CompilerException.of(
                        "Only definition and import/export statements are allowed in file with exports",
                        stmt
                );
            } else if (stmt instanceof DefinitionNode) {
                var def = (DefinitionNode) stmt;
                if (!isAutoInterface(def)) {
                    var name = def instanceof BaseClassNode
                            ? ((TypeNode) def.getName()).strName() : def.getName().toString();
                    var maybeType = linkDefinition(def);
                    if (maybeType.isPresent()) {
                        var type = maybeType.orElseThrow();
                        if (importHandler.getExports().contains(name)) {
                            importHandler.setExportType(name, type);
                        }
                        globals.put(name, type);
                    }
                }
            } else if (stmt instanceof TypedefStatementNode) {
                var tdNode = (TypedefStatementNode) stmt;
                var type = info.getType(tdNode.getType());
                var constant = TypeLoader.typeConstant(tdNode.getLineInfo(), type, info).orElseThrow(
                        () -> CompilerException.of("Cannot get constant for local types", tdNode)
                );
                constants.put(tdNode.getName().strName(), (int) info.constIndex(constant));
            } else if (stmt instanceof DeclaredAssignmentNode) {
                var decl = (DeclaredAssignmentNode) stmt;
                linkDeclaration(decl);
            }
        }
        info.removeStackFrame();
        info.addGlobals(globals, constants);
        return this;
    }

    private Optional<TypeObject> linkDefinition(@NotNull DefinitionNode stmt) {
        var name = stmt.getName();
        if (stmt instanceof FunctionDefinitionNode) {
            var fnNode = (FunctionDefinitionNode) stmt;
            var value = FunctionDefinitionConverter.parseHeader(info, fnNode);
            if (value.isPresent()) {
                var pair = value.orElseThrow();
                var constant = new FunctionConstant(fnNode.getName().getName(), pair.getValue());
                constants.put(fnNode.getName().getName(), (int) info.addConstant(constant));
                return Optional.of(pair.getKey());
            } else {
                return Optional.empty();
            }
        } else if (stmt instanceof PropertyDefinitionNode) {
            var typeNode = ((PropertyDefinitionNode) stmt).getType();
            return Optional.of(info.getType(typeNode));
        } else if (stmt instanceof ContextDefinitionNode) {
            // FIXME: Type for context definitions
            throw CompilerTodoError.of("Context definitions not supported yet", stmt);
        } else if (stmt instanceof OperatorDefinitionNode) {
            throw CompilerException.of("Operator must defined in a class", stmt);
        } else if (stmt instanceof MethodDefinitionNode) {
            throw CompilerException.of("Method must be defined in a class", stmt);
        } else if (stmt instanceof ClassDefinitionNode) {
            var clsNode = (ClassDefinitionNode) stmt;
            var predeclaredType = (StdTypeObject) info.classOf(clsNode.strName()).orElseThrow();
            int index = ClassConverter.completeType(info, clsNode, predeclaredType);
            var constant = new ClassConstant(clsNode.strName(), index, predeclaredType);
            constants.put(clsNode.strName(), (int) info.addConstant(constant));
            return Optional.of(Builtins.TYPE.generify(predeclaredType));
        } else if (stmt instanceof EnumDefinitionNode) {
            var enumNode = (EnumDefinitionNode) stmt;
            var predeclaredType = (StdTypeObject) info.classOf(enumNode.getName().strName()).orElseThrow();
            int index = EnumConverter.completeType(info, enumNode, predeclaredType);
            var constant = new ClassConstant(enumNode.getName().strName(), index, predeclaredType);
            constants.put(enumNode.getName().strName(), (int) info.addConstant(constant));
            return Optional.of(Builtins.TYPE.generify(predeclaredType));
        } else if (stmt instanceof InterfaceDefinitionNode) {
            var interfaceNode = (InterfaceDefinitionNode) stmt;
            var predeclaredType = (InterfaceType) info.classOf(interfaceNode.getName().strName()).orElseThrow();
            int index = InterfaceConverter.completeType(info, interfaceNode, predeclaredType);
            var constant = new ClassConstant(interfaceNode.getName().strName(), index, predeclaredType);
            constants.put(interfaceNode.getName().strName(), (int) info.addConstant(constant));
            return Optional.of(Builtins.TYPE.generify(predeclaredType));
        } else if (stmt instanceof UnionDefinitionNode) {
            var unionNode = (UnionDefinitionNode) stmt;
            var predeclaredType = (UnionTypeObject) info.classOf(unionNode.getName().strName()).orElseThrow();
            int index = UnionConverter.completeType(info, unionNode, predeclaredType);
            var constant = new ClassConstant(unionNode.strName(), index, predeclaredType);
            constants.put(unionNode.strName(), (int) info.addConstant(constant));
            return Optional.of(Builtins.TYPE.generify(predeclaredType));
        } else {
            throw CompilerInternalError.format("Unknown definition %s", stmt, name.getClass());
        }
    }

    private void linkDeclaration(DeclaredAssignmentNode decl) {
        if (decl.getValues().size() == 1 && decl.getTypes().length > 1) {
            throw CompilerTodoError.of("Multiple returns in top-level declared assignment", decl);
        } else if (decl.getValues().size() != decl.getTypes().length) {
            throw CompilerException.of("Number of variables does not match number of values", decl);
        }
        for (int i = 0; i < decl.getTypes().length; i++) {
            var dottedVar = decl.getTypes()[i];
            var value = decl.getValues().get(i);
            if (!dottedVar.getType().isDecided()) {
                throw CompilerException.of("'var' is illegal in top-level declaration", decl);
            } else if (!decl.getValues().getVararg(i).isEmpty()) {
                throw CompilerTodoError.of("Varargs in top-level declared assignment", decl);
            }
            var name = dottedVar.getVariable().getName();
            var type = info.getType(dottedVar.getType());
            globals.put(name, type);
            var constant = TestConverter.constantReturn(value, info, 1);
            if (constant.isPresent()) {
                constants.put(name, (int) info.constIndex(constant.orElseThrow()));
                info.addVariable(name, type, constant.orElseThrow(), decl);
            } else {
                info.addVariable(name, type, decl);
            }
        }
    }

    private boolean isModule(@NotNull TopNode node) {
        for (var stmt : node) {
            if (stmt instanceof ImportExportNode && ((ImportExportNode) stmt).getType() == ImportExportNode.EXPORT) {
                return true;
            }
        }
        return false;
    }

    private boolean isAutoInterface(DefinitionNode stmt) {
        return stmt instanceof InterfaceDefinitionNode && stmt.getDescriptors().contains(DescriptorNode.AUTO);
    }
}
