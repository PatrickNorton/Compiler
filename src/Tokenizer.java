import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.regex.Matcher;


/**
 * The lexer of a file, separates it into a list of tokens.
 * @author Patrick Norton
 */
public class Tokenizer {
    private LinkedList<Token> tokens;

    @Contract(pure = true)
    private Tokenizer() {
        tokens = new LinkedList<>();
    }

    /**
     * Lex a string into a list of tokens.
     * @param str The string to lex
     */
    private void tokenize(String str) {
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

    /**
     * Parse the string passed.
     * @param str The string to lex
     * @return The tokenizer with the list of tokens
     */
    @NotNull
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
