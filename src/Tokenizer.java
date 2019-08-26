import java.util.LinkedList;
import java.util.regex.Matcher;


public class Tokenizer {
    private LinkedList<Token> tokens;

    public Tokenizer() {
        tokens = new LinkedList<>();
    }

    public void tokenize(String str) {
        String s = str;
        tokens.clear();
        while (!s.equals("")) {
            boolean match = false;
            for (TokenType info : TokenType.values()) {
                Matcher m = info.regex.matcher(s);
                if (m.find()) {
                    match = true;
                    if (info != TokenType.WHITESPACE) {
                        String tok = m.group();
                        tokens.add(new Token(info, tok));
                    }
                    s = m.replaceFirst("");
                    break;
                }
            }
            if (!match) throw new RuntimeException(s);
        }
        tokens.add(new Token(TokenType.EPSILON, ""));
    }

    public LinkedList<Token> getTokens() {
        return tokens;
    }

    public static Tokenizer parse(String str) {
        Tokenizer tokenizer = new Tokenizer();
        try {
            tokenizer.tokenize(str);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return tokenizer;
    }
}
