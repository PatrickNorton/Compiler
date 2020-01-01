package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.IndependentNode;
import main.java.parser.ParserException;
import main.java.parser.ParserInternalError;
import main.java.parser.TopNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Converter {
    TopNode node;

    private Converter(TopNode node) {
        this.node = node;
    }

    private void convert() {
        var info = new FileInfo();
        List<IndependentNode> nonDefs = new ArrayList<>();
        List<FunctionDefinitionNode> funcDefs = new ArrayList<>();
        List<DefinitionNode> classDefs = new ArrayList<>();
        for (var node : this.node) {
            if (node instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) node;
                switch (ieNode.getType()) {
                    case "import":
                    case "typeget":
                        addImports(ieNode, info);
                        break;
                    case "export":
                        addExports(ieNode, info);
                        break;
                    default:
                        throw ParserInternalError.of(
                                "Unknown type of import/export", ieNode.getLineInfo()
                        );
                }
            } else if (node instanceof FunctionDefinitionNode) {
                funcDefs.add((FunctionDefinitionNode) node);
            } else if (node instanceof DefinitionNode) {
                classDefs.add((ClassDefinitionNode) node);
            } else {
                nonDefs.add(node);
            }
        }
    }

    private void addImports(@NotNull ImportExportNode node, FileInfo info) {
        assert node.getType().equals("import");
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = node.getAs()[i];
            if (as.isEmpty()) {
                if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                    throw ParserException.of("Illegal import " + value, value);
                }
                info.addImport(((VariableNode) value.getPreDot()).getName());
            } else {
                if (!(as.getPreDot() instanceof VariableNode) || as.getPostDots().length > 0) {
                    throw ParserException.of("Illegal import " + value, value);
                }
                info.addImport(value.toString(), ((VariableNode) as.getPreDot()).getName());
            }
        }
    }

    private void addExports(@NotNull ImportExportNode node, FileInfo info) {
        assert node.getType().equals("export");
        for (int i = 0; i < node.getValues().length; i++) {
            var as = node.getAs()[i];
            var value = node.getValues()[i];
            assert as.isEmpty();
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw ParserException.of("Illegal import " + value, value);
            }
            info.addExport(((VariableNode) value.getPreDot()).getName());
        }
    }

    public static void convert(TopNode node) {
        new Converter(node).convert();
    }
}
