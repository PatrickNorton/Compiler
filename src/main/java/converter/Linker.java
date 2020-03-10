package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContextDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.TopNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class Linker {
    private CompilerInfo info;

    public Linker(CompilerInfo info) {
        this.info = info;
    }

    @NotNull
    public Pair<Map<String, String>, Map<String, TypeObject>> link(@NotNull TopNode node) {
        Map<String, String> exports = new HashMap<>();
        Map<String, TypeObject> globals = new HashMap<>();
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                var name = ((DefinitionNode) stmt).getName();
                TypeObject type;
                if (stmt instanceof FunctionDefinitionNode) {  // TODO: Register functions properly
                    type = Builtins.CALLABLE;
                } else if (stmt instanceof PropertyDefinitionNode) {
                    var typeNode = ((PropertyDefinitionNode) stmt).getType();
                    type = null;  // FIXME: Convert type properly
                } else if (stmt instanceof ContextDefinitionNode) {
                    type = null;
                } else if (stmt instanceof OperatorDefinitionNode) {
                    throw CompilerInternalError.of("Illegal operator definition", stmt);
                } else if (stmt instanceof MethodDefinitionNode) {
                    throw CompilerInternalError.of("Illegal method definition", stmt);
                } else if (stmt instanceof ClassDefinitionNode) {
                    type = Builtins.TYPE;  // FIXME: Generify types correctly
                } else {
                    throw new UnsupportedOperationException(String.format("Unknown definition %s", name.getClass()));
                }
                globals.put(name.toString(), type);
            } else if (stmt instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) stmt;
                switch (ieNode.getType()) {
                    case IMPORT:
                    case TYPEGET:
                        addImports(ieNode, globals);
                        break;
                    case EXPORT:
                        addExports(ieNode, exports);
                        break;
                    default:
                        throw CompilerInternalError.of(
                                "Unknown type of import/export", ieNode.getLineInfo()
                        );
                }
            }
        }
        return Pair.of(exports, globals);
    }

    private void addImports(@NotNull ImportExportNode node, Map<String, TypeObject> globals) {
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
            CompilerInfo f = node.getPreDots() > 0
                    ? Converter.findLocalModule(info.path().getParent(), moduleName)
                    : Converter.findModule(moduleName);
            f.compile().writeToFile(Converter.getDestFile().toPath().resolve(moduleName + ".nbyte").toFile());
            if (globals.containsKey(importName)) {
                throw CompilerException.format("Name %s already defined", node, importName);
            } else {
                globals.put(importName, f.exportType(importName));
                info.addImport(node.getFrom().toString() + "." + value.toString());
            }
        }
    }

    private void addExports(@NotNull ImportExportNode node, Map<String, String> exports) {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean notRenamed = node.getAs().length == 0;
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = notRenamed ? value : node.getAs()[i];
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw CompilerException.of("Illegal export " + value, value);
            }
            var name = ((VariableNode) value.getPreDot()).getName();
            var asName = as.isEmpty() ? name : ((VariableNode) as.getPreDot()).getName();
            if (exports.containsKey(asName)) {
                throw CompilerException.format("Name %s already exported", node, asName);
            } else {
                exports.put(name, asName);
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
