import java.io.File;

public class Compiler {
    public static void main(String[] args) {
        String filename = "/Users/Patricknorton/Projects/Python files/CAS.newlang";
        TokenList tokens = Tokenizer.parse(new File(filename));
        TopNode topNode = Parser.parse(tokens);
    }
}
