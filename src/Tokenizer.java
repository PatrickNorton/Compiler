import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The lexer of a file, separates it into a list of tokens.
 * @author Patrick Norton
 */
public class Tokenizer {
    private LineNumberReader file;
    private String next;
    private static Pattern openComment = Pattern.compile("#\\|((?!\\|#).)*$");
    private static Pattern closeComment = Pattern.compile("^.*?\\|#");
    private static Pattern openString = Pattern.compile("\"((?<!\\\\)\\\\{2}\"|[^\"])*$");
    private static Pattern closeString = Pattern.compile("^((?<!\\\\)\\\\{2}\"|[^\"])*\"");

    @Contract(pure = true)
    private Tokenizer(File name) throws FileNotFoundException {
        file = new LineNumberReader(new FileReader(name));
        next = readLine();
    }

    @Contract(pure = true)
    private Tokenizer(String str) {
        file = new LineNumberReader(new StringReader(str));
        next = readLine();
    }

    /**
     * Get the next token from the tokenized file.
     * @return The next token
     */
    Token tokenizeNext() {
        if (next.isEmpty()) {
            return emptyLine();
        }
        for (TokenType info : TokenType.values()) {
            Matcher match = info.regex.matcher(next);
            if (match.find()) {
                if (info == TokenType.WHITESPACE) {
                    do {
                        next = next.substring(match.end());
                        match = info.regex.matcher(next);
                    } while (match.find());
                } else {
                    next = next.substring(match.end());
                    return new Token(info, match.group());
                }
            }
        }
        throw new ParserException("Syntax error on line " + file.getLineNumber());
    }

    private Token emptyLine() {
        String nextLine = readLine();
        if (nextLine == null) {
            return Token.Epsilon();
        } else {
            Matcher openComment = Tokenizer.openComment.matcher(nextLine);
            final boolean isOpenComment = openComment.find();
            Matcher openString = Tokenizer.openString.matcher(nextLine);
            boolean isOpenString = openString.find();
            boolean commentFirst;
            if (isOpenComment && isOpenString) {
                commentFirst = openComment.start() < openString.start();
            } else if (isOpenComment || isOpenString) {
                commentFirst = isOpenComment;
            } else {
                next = nextLine;
                return Token.Newline();
            }
            StringBuilder nextBuilder = new StringBuilder(nextLine);
            Pattern closeMatcher = commentFirst ? closeComment : closeString;
            String next;
            do {
                next = readLine();
                nextBuilder.append("\n");
                nextBuilder.append(next);
            } while (!closeMatcher.matcher(next).find());
            this.next = nextBuilder.toString();
            return Token.Newline();
        }
    }

    private String readLine() {
        try {
            return file.readLine();
        } catch (IOException e) {
            throw new RuntimeException("File was deleted in mid-parse");
        }
    }

    /**
     * Parse the file passed.
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

    /**
     * Parse the string passed.
     * @param str The string to parse
     * @return The tokenizer with the list of tokens
     */
    @NotNull
    @Contract("_ -> new")
    public static TokenList parse(String str) {
        return new TokenList(new Tokenizer(str));
    }

    /**
     * Create a new empty Tokenizer
     * @return The newly parsed Tokenizer
     */
    @NotNull
    @Contract(" -> new")
    public static Tokenizer empty() {
        return new Tokenizer("");
    }
}
