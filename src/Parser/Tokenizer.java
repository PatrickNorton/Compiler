package Parser;

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
public final class Tokenizer {
    private final LineNumberReader file;
    private String next;
    private static final Pattern OPEN_COMMENT = Pattern.compile("^#\\|((?!\\|#).)*?$");
    private static final Pattern CLOSE_COMMENT = Pattern.compile("^.*?\\|#");
    private static final Pattern OPEN_STRING = Pattern.compile("^[refb]*\"([^\"]|(?<!\\\\)(\\\\{2})*\\\\\")*$");
    private static final Pattern OPEN_SINGLE_STRING = Pattern.compile("^[refb]*'([^']|(?<!\\\\)(\\\\{2})*\\\\')*$");
    private static final Pattern CLOSE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*\"");
    private static final Pattern CLOSE_SINGLE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*'");

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
        adjustForMultiline();
        for (TokenType info : TokenType.values()) {
            Matcher match = info.regex.matcher(next);
            if (match.find()) {
                if (info == TokenType.WHITESPACE) {
                    do {
                        next = next.substring(match.end());
                        if (next.isEmpty()) {
                            return emptyLine();
                        }
                        adjustForMultiline();
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

    /**
     * Return the token for when {@link #next} is empty.
     * @return The empty-line token
     */
    private Token emptyLine() {
        assert next.isEmpty();
        String nextLine = readLine();
        if (nextLine == null) {
            return Token.Epsilon();
        } else {
            next = nextLine;
            return Token.Newline();
        }
    }

    /**
     * Adjust {@link #next} for multiline tokens.
     */
    private void adjustForMultiline() {
        Matcher m = OPEN_COMMENT.matcher(next);
        if (m.find()) {
            concatLines(CLOSE_COMMENT);
            return;
        }
        m = OPEN_STRING.matcher(next);
        if (m.find()) {
            concatLines(CLOSE_STRING);
            return;
        }
        m = OPEN_SINGLE_STRING.matcher(next);
        if (m.find()) {
            concatLines(CLOSE_SINGLE_STRING);
        }
    }

    /**
     * Concatenate lines to {@link #next} until the given pattern matches.
     * @param tillMatch The pattern to match to
     */
    private void concatLines(@NotNull Pattern tillMatch) {
        String next;
        StringBuilder nextBuilder = new StringBuilder(this.next);
        do {
            next = readLine();
            nextBuilder.append('\n');
            nextBuilder.append(next);
        } while (!tillMatch.matcher(next).find());
        this.next = nextBuilder.toString();
    }

    /**
     * Read a line from the line reader
     * @return The read line
     */
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
