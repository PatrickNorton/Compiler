import java.io.BufferedReader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) throws java.io.IOException {
        StringBuilder everything;
        String filename = "/Users/Patricknorton/Projects/Python files/CAS.newlang";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            everything = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                everything.append(line);
                everything.append("\n");
                line = br.readLine();
            }
        }
        Tokenizer tokens = Tokenizer.parse(everything.toString());
        BaseNode base = Parser.parse(tokens.getTokens());
    }
}
