import java.io.BufferedReader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) throws java.io.IOException {
        String everything;
        String filename = "/Users/Patricknorton/Projects/Python files/CAS.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            everything = sb.toString();
        }
        assert false;
        Tokenizer tokens = Tokenizer.parse(everything);
        BaseNode base = Parser.parse(tokens.getTokens());
    }
}
