package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The lexer of a file, separates it into a list of tokens.
 * @author Patrick Norton
 */
public final class Tokenizer {
    private final BufferedReader file;
    private String next;
    private int currentLine;
    private String fullLine;
    private int multilineSize = 0;
    private int lineIndex;
    private boolean lastWasMultiline = false;

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
        file = new BufferedReader(r);
        next = readLine();
        currentLine = 1;
        lineIndex = 0;
        fullLine = next;
    }

    /**
     * Get the next token from the tokenized file.
     * @return The next token
     */
    Token tokenizeNext() {
        Token nextToken;
        do {
            nextToken = getNext();
            if (nextToken == null) {
                throw invalid();
            }
        } while (nextToken.is(TokenType.WHITESPACE));
        return nextToken;
    }

    @Nullable
    private Token getNext() {
        if (next.isEmpty()) {
            return emptyLine();
        }
        adjustForMultiline();
        for (TokenType info : TokenType.values()) {
            Matcher match = info.matcher(next);
            if (match.find()) {
                LineInfo lineInfo = lineInfo();
                next = next.substring(match.end());
                if (lastWasMultiline) {
                    lineIndex = fullLine.length() - next.length() - fullLine.lastIndexOf(System.lineSeparator(), lineIndex);
                } else {
                    lineIndex += next.length();
                }
                lastWasMultiline = false;
                return new Token(info, match.group(), lineInfo);
            }
        }
        return null;
    }

    @NotNull
    private ParserException invalid() {
        for (InvalidToken info : InvalidToken.values()) {
            Matcher match = info.regex.matcher(next);
            if (match.find()) {
                return tokenError(info);
            }
        }
        return tokenError();
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
            next = nextLine.stripTrailing();
            fullLine = next;
            currentLine++;
            currentLine += multilineSize;
            multilineSize = 0;
            lineIndex = 0;
            lastWasMultiline = false;
            appendEscapedLines();
            return Token.Newline(lineInfo());
        }
    }

    /**
     * Adjust {@link #next} for multiline tokens.
     */
    private void adjustForMultiline() {
        if (OPEN_COMMENT.matcher(next).find()) {
            concatLines(CLOSE_COMMENT);
        } else if (OPEN_STRING.matcher(next).find()) {
            concatLines(CLOSE_STRING);
        } else if (OPEN_SINGLE_STRING.matcher(next).find()) {
            concatLines(CLOSE_SINGLE_STRING);
        }
    }

    /**
     * Concatenate lines to {@link #next} until the given pattern matches.
     * @param tillMatch The pattern to match to
     */
    private void concatLines(@NotNull Pattern tillMatch) {
        lastWasMultiline = true;
        String next;
        StringBuilder nextBuilder = new StringBuilder(this.next);
        do {
            next = readLine().stripTrailing();
            nextBuilder.append(System.lineSeparator());
            nextBuilder.append(next);
            multilineSize++;
        } while (!tillMatch.matcher(next).find());
        this.next = nextBuilder.toString();
        fullLine = next;
        appendEscapedLines();
    }

    private void appendEscapedLines() {
        while (next.endsWith("\\")) {
            next = next.substring(0, next.length() - 1) + readLine();
            multilineSize++;
            fullLine = next;
        }
    }

    @Contract(pure = true)
    private int lineIndex() {
        return lineIndex;
    }

    @NotNull
    @Contract(" -> new")
    private LineInfo lineInfo() {
        return new LineInfo(
                currentLine,
                fullLine,
                lineIndex()
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
        return tokenError("Invalid syntax");
    }

    @NotNull
    @Contract("_ -> new")
    private ParserException tokenError(String message) {
        return ParserException.of(message, lineInfo());
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
