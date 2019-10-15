package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
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
    private int currentLine;
    private String fullLine;
    private int multilineSize = 0;

    private static final Pattern OPEN_COMMENT = Pattern.compile("^#\\|((?!\\|#).)*$");
    private static final Pattern CLOSE_COMMENT = Pattern.compile("^.*?\\|#");
    private static final Pattern OPEN_STRING = Pattern.compile("^[refb]*\"([^\"]|(?<!\\\\)(\\\\{2})*\\\\\")*$");
    private static final Pattern OPEN_SINGLE_STRING = Pattern.compile("^[refb]*'([^']|(?<!\\\\)(\\\\{2})*\\\\')*$");
    private static final Pattern CLOSE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*\"");
    private static final Pattern CLOSE_SINGLE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*'");

    @Contract(pure = true)
    private Tokenizer(File name) throws FileNotFoundException {
        this(new FileReader(name));
    }

    @Contract(pure = true)
    private Tokenizer(String str) {
        this(new StringReader(str));
    }

    @Contract(pure = true)
    private Tokenizer(Reader r) {
        file = new LineNumberReader(r);
        next = readLine();
        currentLine = 1;
        fullLine = next;
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
                    return new Token(info, match.group(), lineInfo());
                }
            }
        }
        for (InvalidToken info : InvalidToken.values()) {
            Matcher match = info.regex.matcher(next);
            if (match.find()) {
                throw tokenError(info);
            }
        }
        throw tokenError();
    }

    /**
     * Return the token for when {@link #next} is empty.
     * @return The empty-line token
     */
    private Token emptyLine() {
        assert next.isEmpty();
        String nextLine = readLine();
        if (nextLine == null) {
            return Token.Epsilon(lineInfo());
        } else {
            next = nextLine;
            fullLine = next;
            currentLine++;
            currentLine += multilineSize;
            multilineSize = 0;
            return Token.Newline(lineInfo());
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
            nextBuilder.append(System.lineSeparator());
            nextBuilder.append(next);
            multilineSize++;
        } while (!tillMatch.matcher(next).find());
        this.next = nextBuilder.toString();
        fullLine = next;
    }

    @Contract(pure = true)
    int currentLine() {
        return currentLine;
    }

    private int lineIndex() {
        return fullLine.length() - next.length();
    }

    @NotNull
    @Contract(" -> new")
    private LineInfo lineInfo() {
        return new LineInfo(
                currentLine,
                fullLine,
                fullLine.length() - next.length()
        );
    }

    /**
     * Read a line from the line reader
     * @return The read line
     */
    private String readLine() {
        try {
            return file.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Contract("_ -> new")
    private ParserException tokenError(@NotNull InvalidToken info) {
        return tokenError(info.errorMessage);
    }

    @NotNull
    private ParserException tokenError() {
        return tokenError("");
    }

    @NotNull
    @Contract("_ -> new")
    private ParserException tokenError(String message) {
        return new ParserException(String.format("Error on line %d:%n%s", currentLine(), message));
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
            throw new ParserException("File not found", e);
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
