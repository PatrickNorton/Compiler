package Parser;

import java.io.File;

public class Compiler {
    public static void main(String[] args) {
        TopNode[] nodes = new TopNode[args.length];
        for (int i = 0; i < args.length; i++) {
            nodes[i] = Parser.parse(new File(args[i]));
        }
    }
}
