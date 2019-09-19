package Parser;

import java.io.File;

public class Compiler {
    public static void main(String[] args) {
        String filename = "/Users/Patricknorton/Projects/Python files/CAS.newlang";
        TopNode topNode = Parser.parse(new File(filename));
    }
}
