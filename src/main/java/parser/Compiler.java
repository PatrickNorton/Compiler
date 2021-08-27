package main.java.parser;

import main.java.converter.Converter;
import main.java.converter.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) {
//        TopNode[] nodes = new TopNode[args.length];
//        for (int i = 0; i < args.length; i++) {
//            nodes[i] = main.java.Parser.parse(new File(args[i]));
//        }
        var clargs = CLArgs.parse(args);
        List<File> nodes;
        try {
            nodes = Files.walk(clargs.getTarget())
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (File file : nodes) {
            TopNode node = Parser.parse(file);
            var fileName = file.getName();
            var destFolder = file.toPath().resolveSibling("__ncache__");
            var destFile = destFolder.resolve(fileName.replaceFirst("\\.newlang$", Util.BYTECODE_EXTENSION));
            Converter.convertToFile(destFile.toFile(), node, clargs);
        }
    }
}
