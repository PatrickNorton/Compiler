package main.java.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) {
//        TopNode[] nodes = new TopNode[args.length];
//        for (int i = 0; i < args.length; i++) {
//            nodes[i] = main.java.Parser.parse(new File(args[i]));
//        }
        List<TopNode> nodes;
        try {
            nodes = Files.walk(Paths.get(args[0]))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(Parser::parse)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
