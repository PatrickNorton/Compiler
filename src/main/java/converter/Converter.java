package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.IndependentNode;
import main.java.parser.LineInfo;
import main.java.parser.Parser;
import main.java.parser.ParserException;
import main.java.parser.ParserInternalError;
import main.java.parser.TopNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Converter {
    private TopNode node;
    private FileInfo info;

    private Converter(TopNode node) {
        this.node = node;
        this.info = new FileInfo();
    }

    private void convert() {
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
                info.addImport(value.toString());
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

    public static FileInfo findModule(String name) {
        var path = System.getenv("PATH");
        for (String filename : path.split(":")) {
            try (var walker = Files.walk(Path.of(filename))) {
                var result = walker.filter(Converter::isModule)
                        .collect(Collectors.toList());
                if (!result.isEmpty()) {
                    var endPath = result.get(0);
                    return new Converter(Parser.parse(endPath.toFile())).info;
                }
            } catch (IOException e) {
                // FIXME: Handle this
            }
        }
        throw ParserException.of("Cannot find module " + name, LineInfo.empty());
    }

    private static boolean isModule(Path path) {
        if (Files.isRegularFile(path)) {
            return path.endsWith(".newlang");
        } else {
            var files = path.toFile().list((f, s) -> s.equals("__exports__.newlang"));
            assert files != null;
            return files.length > 0;
        }
    }
}
