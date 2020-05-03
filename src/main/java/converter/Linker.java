package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContextDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
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

public final class Linker {
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

    @Nullable
    private Map<String, TypeObject> declareTypes(@NotNull TopNode node) {
        Map<String, TypeObject> types = new HashMap<>();
        boolean isModule = false;
        for (var stmt : node) {
            if (stmt instanceof ClassDefinitionNode) {
                var cls = (ClassDefinitionNode) stmt;
                if (types.containsKey(cls.strName())) {
                    // TODO: Other LineInfo
                    throw CompilerException.doubleDef(cls.strName(), stmt.getLineInfo(), LineInfo.empty());
                }
                types.put(cls.strName(), new StdTypeObject(cls.strName()));
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
        assert node.getType() == ImportExportNode.IMPORT;
        boolean notRenamed = node.getAs().length == 0;
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
        var file = Converter.getDestFile().toPath().resolve(moduleName + Util.BYTECODE_EXTENSION).toFile();
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

    private void addExports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean notRenamed = node.getAs().length == 0;
        boolean isFrom = !node.getFrom().isEmpty();
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
