package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContextDefinitionNode;
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
import main.java.parser.TopNode;
import main.java.parser.TypedefStatementNode;
import main.java.parser.UnionDefinitionNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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
        info.loadDependents();
        if (!isModule(node)) {
            return this;
        }
        // Filters out auto interfaces, which are registered earlier
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                if (!(stmt instanceof InterfaceDefinitionNode)
                        || !((InterfaceDefinitionNode) stmt).getDescriptors().contains(DescriptorNode.AUTO)) {
                    var def = (DefinitionNode) stmt;
                    var name = def.getName();
                    TypeObject type = linkDefinition(def);
                    globals.put(name.toString(), type);  // FIXME: Use strName instead of toString
                }
            } else if (!(stmt instanceof TypedefStatementNode || stmt instanceof ImportExportNode)) {
                throw CompilerException.of(
                        "Only definition and import/export statements are allowed in file with exports",
                        stmt
                );
            }
        }
        return this;
    }

    private TypeObject linkDefinition(@NotNull DefinitionNode stmt) {
        var name = stmt.getName();
        if (stmt instanceof FunctionDefinitionNode) {  // TODO: Register functions properly
            var fnNode = (FunctionDefinitionNode) stmt;
            var argInfo = ArgumentInfo.of(fnNode.getArgs(), info);
            var fnInfo = new FunctionInfo(argInfo, info.typesOf(fnNode.getRetval()));
            return new FunctionInfoType(fnInfo);
        } else if (stmt instanceof PropertyDefinitionNode) {
            var typeNode = ((PropertyDefinitionNode) stmt).getType();
            return info.getType(typeNode);
        } else if (stmt instanceof ContextDefinitionNode) {
            throw new UnsupportedOperationException();  // FIXME: Type for context definitions
        } else if (stmt instanceof OperatorDefinitionNode) {
            throw CompilerInternalError.of("Operator must defined in a class", stmt);
        } else if (stmt instanceof MethodDefinitionNode) {
            throw CompilerInternalError.of("Method must be defined in a class", stmt);
        } else if (stmt instanceof ClassDefinitionNode) {
            var clsNode = (ClassDefinitionNode) stmt;
            var predeclaredType = (StdTypeObject) info.classOf(clsNode.strName());
            ClassConverter.completeType(info, clsNode, predeclaredType);
            return Builtins.TYPE.generify(predeclaredType);
        } else if (stmt instanceof EnumDefinitionNode) {
            var enumNode = (EnumDefinitionNode) stmt;
            var predeclaredType = (StdTypeObject) info.classOf(enumNode.getName().strName());
            EnumConverter.completeType(info, enumNode, predeclaredType);
            return Builtins.TYPE.generify(predeclaredType);
        } else if (stmt instanceof InterfaceDefinitionNode) {
            var interfaceNode = (InterfaceDefinitionNode) stmt;
            var predeclaredType = (InterfaceType) info.classOf(interfaceNode.getName().strName());
            InterfaceConverter.completeType(info, interfaceNode, predeclaredType);
            return Builtins.TYPE.generify(predeclaredType);
        } else if (stmt instanceof UnionDefinitionNode) {
            var unionNode = (UnionDefinitionNode) stmt;
            var predeclaredType = (StdTypeObject) info.classOf(unionNode.getName().strName());
            UnionConverter.completeType(info, unionNode, predeclaredType);
            return Builtins.TYPE.generify(predeclaredType);
        } else {
            throw new UnsupportedOperationException(String.format("Unknown definition %s", name.getClass()));
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
}
