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
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public static final Set<InterfaceType> ALL_DEFAULT_INTERFACES = new HashSet<>();

    static {
        ALL_DEFAULT_INTERFACES.addAll(Builtins.DEFAULT_INTERFACES);
    }

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
        if (declaredTypes == null) {  // Not a module
            return this;
        }
        info.addPredeclaredTypes(declaredTypes);
        // Done in several steps so that auto interfaces can be registered first
        Deque<InterfaceDefinitionNode> defaultInterfaces = new ArrayDeque<>();
        Deque<DefinitionNode> definitions = new ArrayDeque<>();
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                if (stmt instanceof InterfaceDefinitionNode
                        && ((InterfaceDefinitionNode) stmt).getDescriptors().contains(DescriptorNode.AUTO)) {
                    defaultInterfaces.push((InterfaceDefinitionNode) stmt);
                } else {
                    definitions.push((DefinitionNode) stmt);
                }
            } else if (stmt instanceof ImportExportNode) {
                linkIENode((ImportExportNode) stmt);
            } else if (!(stmt instanceof TypedefStatementNode)) {
                throw CompilerException.of(
                        "Only definition and import/export statements are allowed in file with exports",
                        stmt
                );
            }
        }
        info.addDefaultInterfaces(ALL_DEFAULT_INTERFACES);
        for (var stmt : defaultInterfaces) {
            var name = stmt.getName();
            TypeObject type = linkDefinition(stmt);
            globals.put(name.strName(), type);
        }
        for (var stmt : definitions) {
            var name = stmt.getName();
            TypeObject type = linkDefinition(stmt);
            globals.put(name.toString(), type);  // FIXME: Use strName instead of toString
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

    private void linkIENode(@NotNull ImportExportNode stmt) {
        switch (stmt.getType()) {
            case IMPORT:
            case TYPEGET:
                addImports(stmt);
                break;
            case EXPORT:
                addExports(stmt);
                break;
            default:
                throw CompilerInternalError.of("Unknown type of import/export", stmt);
        }
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
        boolean hasAuto = false;
        Deque<TypedefStatementNode> typedefs = new ArrayDeque<>();
        for (var stmt : node) {
            if (stmt instanceof ClassDefinitionNode) {
                var cls = (ClassDefinitionNode) stmt;
                var strName = cls.strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                var generics = GenericInfo.parse(info, cls.getName().getSubtypes());
                types.put(strName, new StdTypeObject(strName, generics));
                lineInfos.put(strName, cls.getLineInfo());
            } else if (stmt instanceof EnumDefinitionNode) {
                var cls = (EnumDefinitionNode) stmt;
                var strName = cls.getName().strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                var generics = GenericInfo.parse(info, cls.getName().getSubtypes());
                types.put(strName, new StdTypeObject(strName, generics));
                lineInfos.put(strName, cls.getLineInfo());
            } else if (stmt instanceof InterfaceDefinitionNode) {
                var cls = (InterfaceDefinitionNode) stmt;
                var strName = cls.getName().strName();
                if (types.containsKey(strName)) {
                    throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                }
                var generics = GenericInfo.parse(info, cls.getName().getSubtypes());
                var type = new InterfaceType(strName, generics);
                types.put(strName, type);
                lineInfos.put(strName, cls.getLineInfo());
                if (cls.getDescriptors().contains(DescriptorNode.AUTO)) {
                    ALL_DEFAULT_INTERFACES.add(type);
                    hasAuto = true;
                }
            } else if (stmt instanceof ImportExportNode) {
                var ieStmt = (ImportExportNode) stmt;
                // TODO: Types from imports
                if (ieStmt.getType() == ImportExportNode.EXPORT) {
                    isModule = true;
                }
            } else if (stmt instanceof TypedefStatementNode) {
                typedefs.push((TypedefStatementNode) stmt);
            }
        }
        if (!isModule && hasAuto) {
            throw CompilerException.of("Cannot (yet?) have 'auto' interfaces in non-module file", LineInfo.empty());
        }
        for (var stmt : typedefs) {
            var type = stmt.getType();
            var name = stmt.getName();
            types.put(name.strName(), info.getType(type).typedefAs(name.strName()));
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
                ? Converter.findLocalModule(info.path().getParent(), moduleName, node)
                : Converter.findModule(moduleName);
        var file = Converter.resolveFile(moduleName);
        f.compile(file);  // FIXME: Circular imports cause stack overflow
        if (globals.containsKey(importName)) {
            throw CompilerException.format("Name %s already defined", node, importName);
        }
        var exportType = f.importHandler().exportType(importName);
        if (exportType == null) {
            throw CompilerException.format("'%s' not exported in module '%s'", node, importName, moduleName);
        }
        globals.put(importName, exportType);
        var name = node.getFrom().isEmpty() ? importName : node.getFrom().toString() + "." + importName;
        info.importHandler().addImport(name);
    }

    private void addWildcardImport(String moduleName, @NotNull ImportExportNode node) {
        CompilerInfo f = node.getPreDots() > 0
                ? Converter.findLocalModule(info.path().getParent(), moduleName, node)
                : Converter.findModule(moduleName);
        var file = Converter.resolveFile(moduleName);
        f.compile(file);
        for (var name : f.importHandler().getExports()) {
            globals.put(name, f.importHandler().exportType(name));
            info.importHandler().addImport(moduleName + "." + name);
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
            addWildcardExport(moduleName, node);
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

    private void addWildcardExport(String moduleName, @NotNull ImportExportNode node) {
        CompilerInfo f = node.getPreDots() > 0
                ? Converter.findLocalModule(info.path().getParent(), moduleName, node)
                : Converter.findModule(moduleName);
        var file = Converter.resolveFile(moduleName);
        f.compile(file);
        for (var name : f.importHandler().getExports()) {
            globals.put(name, f.importHandler().exportType(name));
            info.importHandler().addImport(moduleName + "." + name);
            if (exports.containsKey(name)) {
                throw CompilerException.format("Name %s already exported", node, name);
            } else {
                exports.put(name, Pair.of(name, node.getLineInfo()));
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
