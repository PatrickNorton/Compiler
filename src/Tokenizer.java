import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The lexer of a file, separates it into a list of tokens.
 * @author Patrick Norton
 */
public class Tokenizer {
    private Scanner file;
    private static Pattern delimiter = Pattern.compile(
            "(?=#\\|((?!#\\|).|\n)*#\\||#(?!\\|).*|(?<![bfre])[bfre]*\"([^\"]|\\\\\"|\n)+(?<!\\\\)(\\\\{2})*\"|(?<!\\\\)\\R)", Pattern.MULTILINE);
    private String next;

    @Contract(pure = true)
    private Tokenizer(File name) throws FileNotFoundException {
        file = new Scanner(name).useDelimiter(delimiter);
        next = file.next();
    }

    Tokenizer(String str) {
        file = new Scanner(str).useDelimiter(delimiter);
        next = file.next();
    }

    Token tokenizeNext() {
        while (next.isEmpty()) {
            if (file.hasNext()) {
                next = file.next();
                String a = "";
            } else {
                return new Token(TokenType.EPSILON, "");
            }
        }
        for (TokenType info : TokenType.values()) {
            Matcher match = info.regex.matcher(next);
            if (match.find()) {
                if (info == TokenType.WHITESPACE) {
                    do {
                        next = next.substring(match.end());
                        match = info.regex.matcher(next);
                    } while (match.find());
                } else if (info == TokenType.EPSILON) {
                    if (file.hasNext()) {
                        next = file.next();
                        return tokenizeNext();
                    } else {
                        return new Token(TokenType.EPSILON, "");
                    }
                } else {
                    next = next.substring(match.end());
                    return new Token(info, match.group());
                }
            }
        }
        throw new ParserException("Syntax error");
    }

    /**
     * Parse the string passed.
     * @param f The file to pass
     * @return The tokenizer with the list of tokens
     */
    @Contract("_ -> new")
    @NotNull
    public static TokenList parse(File f) {
        Tokenizer tokenizer;
        try {
            tokenizer = new Tokenizer(f);
        } catch (FileNotFoundException e) {
            throw new ParserException("File not found");
        }
        return new TokenList(tokenizer);
    }

    public static TokenList parse(String str) {
        return new TokenList(new Tokenizer(str));
    }
}
