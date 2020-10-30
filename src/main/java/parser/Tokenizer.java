package main.java.parser;

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
import java.util.Optional;
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

    private Tokenizer(File name) throws FileNotFoundException {
        this(new FileReader(name), name.toPath(), 0);
    }

    private Tokenizer(String str, Path path, int lineNo) {
        this(new StringReader(str), path, lineNo);
    }

    private Tokenizer(Reader r, Path path, int lineNo) {
        file = new LineNumberReader(r);
        file.setLineNumber(lineNo);
        next = readLine();
        if (next == null) next = "";
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
            nextToken = getNext().orElseThrow(this::invalid);
        } while (nextToken.is(TokenType.WHITESPACE));
        return nextToken;
    }

    private Optional<Token> getNext() {
        if (next.isEmpty()) {
            return Optional.of(emptyLine());
        }
        Optional<Token> nextToken = adjustForMultiline();
        if (nextToken.isPresent()) {
            return nextToken;
        }
        if (next.startsWith("|#")) {  // Special-case, prevent it being parsed separately
            throw ParserException.of(
                    "Illegal sequence '|#': may only appear as the end of a comment", lineInfo()
            );
        }
        for (TokenType info : TokenType.values()) {
            Matcher match = info.matcher(next);
            if (match.find()) {
                LineInfo lineInfo = lineInfo();
                next = next.substring(match.end());
                return Optional.of(new Token(info, match.group(), lineInfo));
            }
        }
        return Optional.empty();
    }

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

    private Optional<Token> adjustForMultiline() {
        if (OPEN_COMMENT.matcher(next).find()) {
            return Optional.of(concatLines(CLOSE_COMMENT, TokenType.WHITESPACE));
        } else if (OPEN_STRING.matcher(next).find()) {
            return Optional.of(concatLines(CLOSE_STRING, TokenType.STRING));
        } else if (OPEN_SINGLE_STRING.matcher(next).find()) {
            return Optional.of(concatLines(CLOSE_SINGLE_STRING, TokenType.STRING));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Concatenate lines to {@link #next} until the given pattern matches.
     * @param tillMatch The pattern to match to
     */

    private Token concatLines(Pattern tillMatch, TokenType resultType) {
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

    private int lineIndex() {
        if (lbIndices.isEmpty()) return fullLine.length() - next.length();
        Integer index = lbIndices.lower(fullLine.length() - next.length());
        return fullLine.length() - next.length() - (index == null ? 0 : index);
    }

    private int lineNumber() {
        return file.getLineNumber() + (lbIndices.isEmpty() ? 0 :
                lbIndices.headSet(fullLine.length() - next.length(), true).size());
    }

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

    private ParserException tokenError(InvalidToken info) {
        return tokenError(info.errorMessage);
    }

    private ParserException tokenError() {
        return tokenError("Invalid syntax");
    }

    private ParserException tokenError(String message) {
        return ParserException.of(message, lineInfo());
    }

    /**
     * Parse the file passed.
     * @param f The file to pass
     * @return The tokenizer with the list of tokens
     */

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

    public static TokenList parse(String str, Path path, int lineNo) {
        return new TokenList(new Tokenizer(str, path, lineNo));
    }
}
