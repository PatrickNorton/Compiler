package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The lexer of a file, separates it into a list of tokens.
 * @author Patrick Norton
 */
public final class Tokenizer {
    private final LineNumberReader file;
    private final Path fileName;
    private String next;
    private String fullLine;
    private NavigableSet<Integer> lbIndices;

    private static final Pattern OPEN_COMMENT = Pattern.compile("^#\\|((?!\\|#).)*$");
    private static final Pattern CLOSE_COMMENT = Pattern.compile("^.*?\\|#");
    private static final Pattern OPEN_STRING = Pattern.compile("^[refb]*\"([^\"]|(?<!\\\\)(\\\\{2})*\\\\\")*$");
    private static final Pattern OPEN_SINGLE_STRING = Pattern.compile("^[refb]*'([^']|(?<!\\\\)(\\\\{2})*\\\\')*$");
    private static final Pattern CLOSE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*\"");
    private static final Pattern CLOSE_SINGLE_STRING = Pattern.compile("^.*?(?<!\\\\)(\\\\{2})*'");

    @Contract(pure = true)
    private Tokenizer(File name) throws FileNotFoundException {
        this(new FileReader(name), name.toPath());
    }

    @Contract(pure = true)
    private Tokenizer(String str) {
        this(new StringReader(str), Path.of(""));
    }

    @Contract(pure = true)
    private Tokenizer(Reader r, Path path) {
        file = new LineNumberReader(r);
        next = readLine();
        fullLine = next;
        fileName = path;
        lbIndices = new TreeSet<>();
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
        Token nextToken = adjustForMultiline();
        if (nextToken != null) {
            return nextToken;
        }
        for (TokenType info : TokenType.values()) {
            Matcher match = info.matcher(next);
            if (match.find()) {
                LineInfo lineInfo = lineInfo();
                next = next.substring(match.end());
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
            next = Normalizer.normalize(nextLine.stripTrailing(), Normalizer.Form.NFKD);
            fullLine = next;
            lbIndices.clear();
            appendEscapedLines();
            return Token.Newline(lineInfo());
        }
    }

    /**
     * Adjust {@link #next} for multiline tokens.
     */
    @Nullable
    private Token adjustForMultiline() {
        if (OPEN_COMMENT.matcher(next).find()) {
            return concatLines(CLOSE_COMMENT, TokenType.WHITESPACE);
        } else if (OPEN_STRING.matcher(next).find()) {
            return concatLines(CLOSE_STRING, TokenType.STRING);
        } else if (OPEN_SINGLE_STRING.matcher(next).find()) {
            return concatLines(CLOSE_SINGLE_STRING, TokenType.STRING);
        } else {
            return null;
        }
    }

    /**
     * Concatenate lines to {@link #next} until the given pattern matches.
     * @param tillMatch The pattern to match to
     */
    @NotNull
    @Contract("_, _ -> new")
    private Token concatLines(@NotNull Pattern tillMatch, TokenType resultType) {
        LineInfo lineInfo = lineInfo();
        StringBuilder nextSequence = new StringBuilder(next);
        while (true) {
            String nextLine = readLine();
            if (nextLine == null) {
                throw ParserException.of("Unmatched delimiter", lineInfo);
            }
            nextLine = Normalizer.normalize(nextLine.stripTrailing(), Normalizer.Form.NFKD);
            nextSequence.append(System.lineSeparator());
            Matcher m = tillMatch.matcher(nextLine);
            if (m.find()) {
                nextSequence.append(m.group());
                fullLine = nextLine;
                next = nextLine.substring(m.group().length());
                appendEscapedLines();
                return new Token(resultType, nextSequence.toString(), lineInfo);
            }
            nextSequence.append(nextLine);
        }
    }

    private void appendEscapedLines() {
        while (next.endsWith("\\")) {
            next = next.substring(0, next.length() - 1) + readLine();
            fullLine = next;
            lbIndices.add(fullLine.length());
        }
    }

    @Contract(pure = true)
    private int lineIndex() {
        if (lbIndices.isEmpty()) return fullLine.length() - next.length();
        Integer index = lbIndices.lower(fullLine.length() - next.length());
        return fullLine.length() - next.length() - (index == null ? 0 : index);
    }

    @Contract(pure = true)
    private int lineNumber() {
        return file.getLineNumber() + (lbIndices.isEmpty() ? 0 :
                lbIndices.headSet(fullLine.length() - next.length(), true).size());
    }

    @NotNull
    @Contract(" -> new")
    private LineInfo lineInfo() {
        return new LineInfo(
                fileName,
                lineNumber(),
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
}
