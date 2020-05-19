package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContextDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.LineInfo;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.TopNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    public Linker(CompilerInfo info) {
        this.info = info;
        this.exports = new HashMap<>();
        this.globals = new HashMap<>();
    }

    public Map<String, Pair<String, LineInfo>> getExports() {
        return exports;
    }

    public Map<String, TypeObject> getGlobals() {
        return globals;
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
        assert exports.isEmpty() && globals.isEmpty();
        var declaredTypes = declareTypes(node);
        if (declaredTypes == null) {  // declaredTypes is null if not a module
            return this;
        }
        info.addPredeclaredTypes(declaredTypes);
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                var name = ((DefinitionNode) stmt).getName();
                TypeObject type;
                if (stmt instanceof FunctionDefinitionNode) {  // TODO: Register functions properly
                    var fnNode = (FunctionDefinitionNode) stmt;
                    var argInfo = ArgumentInfo.of(fnNode.getArgs(), info);
                    var fnInfo = new FunctionInfo(argInfo, info.typesOf(fnNode.getRetval()));
                    type = new FunctionInfoType(fnInfo);
                } else if (stmt instanceof PropertyDefinitionNode) {
                    var typeNode = ((PropertyDefinitionNode) stmt).getType();
                    type = info.getType(typeNode);
                } else if (stmt instanceof ContextDefinitionNode) {
                    type = null;  // FIXME: Type for context definitions
                } else if (stmt instanceof OperatorDefinitionNode) {
                    throw CompilerInternalError.of("Operator must defined in a class", stmt);
                } else if (stmt instanceof MethodDefinitionNode) {
                    throw CompilerInternalError.of("Method must be defined in a class", stmt);
                } else if (stmt instanceof ClassDefinitionNode) {
                    var clsNode = (ClassDefinitionNode) stmt;
                    var predeclaredType = (StdTypeObject) info.classOf(clsNode.strName());
                    ClassConverter.completeType(info, clsNode, predeclaredType);
                    type = Builtins.TYPE.generify(predeclaredType);
                } else if (stmt instanceof EnumDefinitionNode) {
                    var enumNode = (EnumDefinitionNode) stmt;
                    var predeclaredType = (StdTypeObject) info.classOf(enumNode.getName().strName());
                    EnumConverter.completeType(info, enumNode, predeclaredType);
                    type = Builtins.TYPE.generify(predeclaredType);
                } else if (stmt instanceof InterfaceDefinitionNode) {
                    var interfaceNode = (InterfaceDefinitionNode) stmt;
                    var predeclaredType = (InterfaceType) info.classOf(interfaceNode.getName().strName());
                    InterfaceConverter.completeType(info, interfaceNode, predeclaredType);
                    type = Builtins.TYPE.generify(predeclaredType);
                } else {
                    throw new UnsupportedOperationException(String.format("Unknown definition %s", name.getClass()));
                }
                globals.put(name.toString(), type);
            } else if (stmt instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) stmt;
                switch (ieNode.getType()) {
                    case IMPORT:
                    case TYPEGET:
                        addImports(ieNode);
                        break;
                    case EXPORT:
                        addExports(ieNode);
                        break;
                    default:
                        throw CompilerInternalError.of(
                                "Unknown type of import/export", ieNode.getLineInfo()
                        );
                }
            } else {
                throw CompilerException.of(
                        "Only definition and import/export statements are allowed in file with exports",
                        stmt
                );
            }
        }
        return this;
    }

    /**
     * Return a map with all pre-declared types in the file.
     * If the file is not a module (has no {@link ImportExportNode 'export'
     * statements}), return null.
     * <p>
     * Pre-declared types have no associated attributes, and are there solely
     * to assist in linking in the second pass, where they will be filled out.
     *
     * @param node The {@link TopNode} to run through
     * @return The map of types, or null if not a module
     */
    @Nullable
    private Map<String, TypeObject> declareTypes(@NotNull TopNode node) {
        Map<String, TypeObject> types = new HashMap<>();
        Map<String, LineInfo> lineInfos = new HashMap<>();
        boolean isModule = false;
        for (var stmt : node) {
            if (stmt instanceof ClassDefinitionNode) {
                var cls = (ClassDefinitionNode) stmt;
                var strName = cls.strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                types.put(strName, new StdTypeObject(strName));
                lineInfos.put(strName, cls.getLineInfo());
            } else if (stmt instanceof EnumDefinitionNode) {
                var cls = (EnumDefinitionNode) stmt;
                var strName = cls.getName().strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                types.put(strName, new StdTypeObject(strName));
                lineInfos.put(strName, cls.getLineInfo());
            } else if (stmt instanceof InterfaceDefinitionNode) {
                var cls = (InterfaceDefinitionNode) stmt;
                var strName = cls.getName().strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                types.put(strName, new InterfaceType(strName));
                lineInfos.put(strName, cls.getLineInfo());
            } else if (stmt instanceof ImportExportNode) {
                var ieStmt = (ImportExportNode) stmt;
                // TODO: Types from imports
                if (ieStmt.getType() == ImportExportNode.EXPORT) {
                    isModule = true;
                }
            }
        }
        return isModule ? types : null;
    }

    private void addImports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        boolean notRenamed = node.getAs().length == 0;
        if (node.isWildcard()) {
            addWildcardImport(moduleName(node, 0), node);
            return;
        }
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = notRenamed ? value : node.getAs()[i];
            String moduleName = moduleName(node, i);
            if (!(as.getPreDot() instanceof VariableNode) || as.getPostDots().length > 0) {
                throw CompilerException.of("Illegal import " + as, as);
            }
            String importName = value.toString();
            addImport(moduleName, importName, node);

        }
    }

    private void addImport(String moduleName, String importName, @NotNull ImportExportNode node) {
        CompilerInfo f = node.getPreDots() > 0
                ? Converter.findLocalModule(info.path().getParent(), moduleName)
                : Converter.findModule(moduleName);
        var file = Converter.resolveFile(moduleName);
        f.compile(file);
        if (globals.containsKey(importName)) {
            throw CompilerException.format("Name %s already defined", node, importName);
        }
        var exportType = f.exportType(importName);
        if (exportType == null) {
            throw CompilerException.format("'%s' not exported in module '%s'", node, importName, moduleName);
        }
        globals.put(importName, exportType);
        info.addImport(node.getFrom().isEmpty() ? importName : node.getFrom().toString() + "." + importName);
    }

    private void addWildcardImport(String moduleName, @NotNull ImportExportNode node) {
        CompilerInfo f = node.getPreDots() > 0
                ? Converter.findLocalModule(info.path().getParent(), moduleName)
                : Converter.findModule(moduleName);
        var file = Converter.resolveFile(moduleName);
        f.compile(file);
        for (var name : f.getExports()) {
            globals.put(name, f.exportType(name));
            info.addImport(moduleName + "." + name);
        }
    }

    private void addExports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean notRenamed = node.getAs().length == 0;
        boolean isFrom = !node.getFrom().isEmpty();
        if (node.isWildcard()) {
            if (node.getFrom().isEmpty()) {
                throw CompilerException.of("Cannot 'export *' without a 'from' clause", node);
            }
            var moduleName = moduleName(node, 0);
            addWildcardImport(moduleName, node);
            return;
        }
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            if (isFrom) {
                addImport(moduleName(node, i), value.toString(), node);
            }
            var as = notRenamed ? value : node.getAs()[i];
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw CompilerException.of("Illegal export " + value, value);
            }
            var name = ((VariableNode) value.getPreDot()).getName();
            var asName = as.isEmpty() ? name : ((VariableNode) as.getPreDot()).getName();
            if (exports.containsKey(asName)) {
                throw CompilerException.format("Name %s already exported", node, asName);
            } else {
                exports.put(name, Pair.of(asName, node.getLineInfo()));
            }
        }
    }

    private static String moduleName(@NotNull ImportExportNode node, int i) {
        if (!node.getFrom().isEmpty()) {
            return ((VariableNode) node.getFrom().getPreDot()).getName();
        } else {
            return ((VariableNode) node.getValues()[i].getPreDot()).getName();
        }
    }
}
