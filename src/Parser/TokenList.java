package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import util.CircularBuffer;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The list of tokens.
 * @author Patrick Norton
 */
public final class TokenList implements Iterable<Token> {
    private final CircularBuffer<Token> buffer;
    private final Tokenizer tokenizer;

    /**
     * Construct a new instance of TokenList.
     * <p>
     *     This takes a {@link Tokenizer} as input and slowly parses that until
     *     there are no tokens left.
     * </p>
     * @param tokenizer The tokenizer to collect from
     */
    @Contract(pure = true)
    public TokenList(Tokenizer tokenizer) {
        this.buffer = new CircularBuffer<Token>();
        this.tokenizer = tokenizer;
    }

    /**
     * Ensure the buffer is a certain size
     * @param length The length to ensure the buffer is
     */
    private void ensureLength(int length) {
        while (buffer.size() <= length) {
            buffer.add(tokenizer.tokenizeNext());
        }
    }

    /**
     * Test if a line contains a certain type of token.
     * @param question The questions to test if the line contains
     * @return If the line contains that token
     */
    boolean lineContains(TokenType... question) {
        for (Token token : this.lineIterator()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if a line contains a certain type of token.
     * @param question The questions to test if the line contains
     * @return If the line contains that token
     */
    boolean lineContains(String... question) {
        for (Token token : this.lineIterator()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if a line contains a certain type of token.
     * @param question The questions to test if the line contains
     * @return If the line contains that token
     */
    boolean lineContains(Keyword... question) {
        for (Token token : this.lineIterator()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create an iterable for a line of the code.
     * @return The iterable for the line
     */
    @NotNull
    @Contract(pure = true)
    private Iterable<Token> lineIterator() {
        return LineIterator::new;
    }

    private class LineIterator implements Iterator<Token> {
        private Iterator<Token> iterator = TokenList.this.zerothLevel().iterator();
        private boolean hasNext = iterator.hasNext();
        private Token next = hasNext ? iterator.next() : null;
        private Token previous = null;

        @Override
        public boolean hasNext() {
            return next == null ? adjustNext() : hasNext;
        }

        @Override
        public Token next() {
            if (next == null) {
                adjustNext();
            }
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            previous = next;
            next = null;
            return previous;
        }

        private boolean adjustNext() {
            assert next == null;
            if (!iterator.hasNext()) {
                return hasNext = false;
            } else {
                next = iterator.next();
            }
            if (next.is("{") && !previous.is(TokenType.BRACE_IS_LITERAL)) {
                hasNext = false;
            } else if (previous.is(TokenType.NEWLINE)) {
                hasNext = false;
            }
            return hasNext;
        }
    }

    /**
     * The list of tokens within the first brace found
     * @return The list of tokens
     */
    @NotNull
    @Contract(pure = true)
    private Iterable<Token> firstLevel() {
        return FirstLevelIterator::new;
    }

    @NotNull
    @Contract(pure = true)
    private Iterable<Token> zerothLevel() {
        return ZerothLevelIterator::new;
    }

    /**
     * Check whether or not the open brace contains a token of a certain type.
     * @param question The token types to test for
     * @return Whether or not that token is contained in the brace
     */
    boolean braceContains(TokenType... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether or not the open brace contains a token of a certain type.
     * @param question The token types to test for
     * @return Whether or not that token is contained in the brace
     */
    boolean braceContains(String... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether or not the open brace contains a token of a certain type.
     * @param question The token types to test for
     * @return Whether or not that token is contained in the brace
     */
    boolean braceContains(Keyword... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    boolean braceIsEmpty() {
        assert tokenIs(TokenType.OPEN_BRACE);
        int next = 1;
        while (tokenIs(1, TokenType.NEWLINE)) {
            next++;
        }
        return tokenIs(next, TokenType.CLOSE_BRACE);
    }

    /**
     * Get the size of the variable at the start of the list of tokens.
     * @return The size of the variable
     */
    int sizeOfVariable() {
        return sizeOfVariable(0);
    }

    /**
     * Get the size of the variable at the start of the list of tokens.
     * @param offset The place to start at
     * @return The size of the variable
     */
    int sizeOfVariable(int offset) {
        assert tokenIs(offset, TokenType.NAME, TokenType.OPEN_BRACE);
        int netBraces = 0;
        boolean wasVar = false;
        for (int size = offset;; size++) {
            Token token = getToken(size);
            switch (token.token) {
                case OPEN_BRACE:
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    if (netBraces == 0) {
                        return size;
                    }
                    netBraces--;
                    break;
                case NAME:
                    if (wasVar && netBraces == 0) {
                        return size;
                    }
                    wasVar = true;
                    break;
                case DOT:
                    wasVar = false;
                    break;
                case EPSILON:
                    if (netBraces > 0) {
                        throw ParserException.of("Unmatched brace", token);
                    }  // Intentional fallthrough here
                default:
                    if (netBraces == 0) {
                        return size;
                    }
            }
        }
    }

    /**
     * The size of the brace beginning at the specified offset.
     * @param offset The offset to start at
     * @return The size of the brace
     */
    int sizeOfBrace(int offset) {
        int netBraces = 0;
        int size = offset;
        for (Token token : this.iteratorFrom(offset)) {
            if (token.is(TokenType.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(TokenType.CLOSE_BRACE)) {
                netBraces--;
            }
            size++;
            if (netBraces == 0) {
                break;
            }
        }
        return size;
    }

    /**
     * The size of the return values of a statement.
     * @param offset The number of tokens from the start the return is
     * @return The size of the returned values
     */
    int sizeOfReturn(int offset) {
        int size = offset;
        assert tokenIs(offset, TokenType.ARROW);
        size++;
        while (tokenIs(size, TokenType.NAME)) {
            size = sizeOfVariable(size);
            if (tokenIs(size, TokenType.COMMA)) {
                size++;
            } else {
                break;
            }
        }
        return size;
    }

    /**
     * Expect and parse a newline from the beginning of the list.
     */
    public void Newline() {
        expect(TokenType.NEWLINE, "newline", true);
    }

    /**
     * Remove all leading newlines from the list.
     */
    public void passNewlines() {
        while (tokenIs(TokenType.NEWLINE)) {
            nextToken();
        }
    }

    /**
     * Get the token at an index.
     * @param index The index to get the token at
     * @return The token at the index
     */
    public Token getToken(int index) {
        ensureLength(index);
        return buffer.get(index);
    }

    /**
     * Get the first token in the queue.
     * @return The first token
     */
    public Token getFirst() {
        ensureLength(0);
        return buffer.getFirst();
    }

    /**
     * Test if the first token is the TokenType given.
     * @param type The type to test if the token is
     * @return Whether the token is that type
     */
    public boolean tokenIs(TokenType type) {
        return getFirst().is(type);
    }

    /**
     * Test if the first token is one of a series of types.
     * @param types The types to check against
     * @return Whether or not the token is of that type
     */
    public boolean tokenIs(TokenType... types) {
        return getFirst().is(types);
    }

    /**
     * Test if the token's sequence matches the one given.
     * @param sequence The sequence to test
     * @return Whether or not the sequences match
     */
    public boolean tokenIs(String sequence) {
        return getFirst().is(sequence);
    }

    /**
     * Test if the first token is one of a series of values.
     * @param types The values to check against
     * @return Whether or not the token is of that value
     */
    public boolean tokenIs(String... types) {
        return getFirst().is(types);
    }

    /**
     * Test if the token at the specified location is of the type given
     * @param index The index of the token
     * @param type The type to test
     * @return If the token is of that type
     */
    public boolean tokenIs(int index, TokenType type) {
        return getToken(index).is(type);
    }

    /**
     * Test if the token at the index is one of a certain set of types.
     * @param index The index to check for type
     * @param types The types to check against
     * @return Whether the token is of those types
     */
    public boolean tokenIs(int index, TokenType... types) {
        return getToken(index).is(types);
    }

    /**
     * Test if the token at the specified location is of the sequence given
     * @param index The index of the token
     * @param sequence The sequence to test
     * @return If the token is of that type
     */
    public boolean tokenIs(int index, String sequence) {
        return getToken(index).is(sequence);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     * @param index The index to check for value
     * @param types The values to check against
     * @return Whether the token is of those values
     */
    public boolean tokenIs(int index, String... types) {
        return getToken(index).is(types);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     * @param type1 The TokenType value to check
     * @param type2 The String value to check
     * @return Whether the token is of those values
     */
    public boolean tokenIs(TokenType type1, String type2) {
        return getFirst().is(type1) || getFirst().is(type2);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     * @param type1 The TokenType value to check
     * @param type2 The Keyword value to check
     * @return Whether the token is of those values
     */
    public boolean tokenIs(TokenType type1, Keyword type2) {
        return getFirst().is(type1) || getFirst().is(type2);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     * @param type1 The TokenType value to check
     * @param types The Keyword values to check
     * @return Whether the token is of those values
     */
    public boolean tokenIs(TokenType type1, Keyword... types) {
        return getFirst().is(type1) || getFirst().is(types);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     * @param type1 The String value to check
     * @param type2 The Keyword value to check
     * @return Whether the token is of those values
     */
    public boolean tokenIs(String type1, Keyword type2) {
        return getFirst().is(type1) || getFirst().is(type2);
    }

    /**
     * Test if the token at the index is one of a certain set of values.
     *
     * @param type1 The TokenType value to check
     * @param type2 The String value to check
     * @return Whether the token is of those values
     */
    public boolean tokenIs(int index, TokenType type1, String type2) {
        return getToken(index).is(type1) || getToken(index).is(type2);
    }

    /**
     * Test if the first token is a keyword of the type given
     * @param type The type to test
     * @return If the token is of that type
     */
    public boolean tokenIs(Keyword type) {
        return getFirst().is(type);
    }

    /**
     * Check if the token is a keyword of a certain type.
     * @param types The keyword types to test
     * @return Whether the token is of that type
     */
    public boolean tokenIs(Keyword... types) {
        return getFirst().is(types);
    }

    /**
     * Test if the token at the specified location is a keyword of the type
     * given.
     * @param index The index of the token
     * @param type The keyword to test
     * @return If the token is that keyword
     */
    public boolean tokenIs(int index, Keyword type) {
        return getToken(index).is(type);
    }

    /**
     * Check if the token is a keyword of a certain type.
     * @param index The index of the keyword to check
     * @param types The keyword types to test
     * @return Whether the token is of that type
     */
    public boolean tokenIs(int index, Keyword... types) {
        return getToken(index).is(types);
    }

    /**
     * Test if the first token is one of the types given.
     *
     * @param types The types to check
     * @return Whether or not it is one of those types
     */
    public boolean tokenIs(Set<TokenType> types) {
        return getFirst().is(types);
    }

    /**
     * Test if the first token is one of the keywords given.
     *
     * @param types The keywords to check
     * @return Whether or not it is one of those keywords
     */
    public boolean tokenIsKeyword(Set<Keyword> types) {
        return getFirst().isKeyword(types);
    }

    /**
     * The sequence of the first token.
     *
     * @return The sequence
     */
    public String tokenSequence() {
        return getFirst().sequence;
    }

    /**
     * The type of the first token.
     *
     * @return The type
     */
    public TokenType tokenType() {
        return getFirst().token;
    }

    /**
     * The number of newlines from the given position in the list.
     *
     * @param start Where to start
     * @return The number of newlines
     */
    public int numberOfNewlines(int start) {
        int count = 0;
        while (tokenIs(start + count, TokenType.NEWLINE)) {
            count++;
        }
        return count;
    }

    /**
     * Pop the first token and move on.
     */
    public void nextToken() {
        if (!buffer.isEmpty()) {
            buffer.pop();
        } else {
            tokenizer.tokenizeNext();
        }
    }

    /**
     * Pop the first token and move on, possibly ignoring newlines.
     * @param ignoreNewlines Whether or not to ignore newlines
     */
    public void nextToken(boolean ignoreNewlines) {
        nextToken();
        if (ignoreNewlines) {
            passNewlines();
        }
    }

    /**
     * The iterator for a TokenList.
     */
    private class TokenIterator implements Iterator<Token> {
        private final ListIterator<Token> bufferIterator;
        private boolean done = false;

        private TokenIterator() {
            bufferIterator = buffer.listIterator();
        }

        private TokenIterator(int i) {
            ensureLength(i);
            bufferIterator = buffer.listIterator(i);
        }

        @Override
        public boolean hasNext() {
            if (!bufferIterator.hasNext()) {
                if (done) {
                    return false;
                }
                buffer();
            }
            return true;
        }

        @Override
        public Token next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return bufferIterator.next();
        }

        /**
         * Buffer the iterator if next is unknown
         */
        private void buffer() {
            if (done) {
                throw new NoSuchElementException();
            }
            Token next = tokenizer.tokenizeNext();
            bufferIterator.add(next);
            bufferIterator.previous();
            if (next.is(TokenType.EPSILON)) {
                done = true;
            }
        }
    }

    private abstract class LevelIterator implements Iterator<Token> {
        private int netBraces = 0;
        private final int numBraces;
        private int beginning;
        private Token previous;
        private final Iterator<Token> iterator = TokenList.this.iterator();

        LevelIterator(int level) {
            this.numBraces = level;
            this.beginning = numBraces;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext() && (beginning > 0 || netBraces >= numBraces);
        }

        @Override
        public Token next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (beginning > 0) {
                beginning--;
            }
            Token next;
            do {
                next = iterator.next();
                adjustBraces(next);
                previous = next;
            } while (netBraces > numBraces);
            return next;
        }

        /**
         * Adjust the net number of braces to match the token given.
         * @param token The token to adjust based on
         */
        private void adjustBraces(@NotNull Token token) {
            switch (token.token) {
                case OPEN_BRACE:
                    if (nextIsBlockBrace(token)) {
                        break;
                    }
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    netBraces--;
                    break;
                case EPSILON:
                    if (netBraces > 0) {
                        throw ParserException.of("Unmatched brace", token);
                    } else {
                        netBraces--;
                        break;
                    }
            }
        }

        private boolean nextIsBlockBrace(Token token) {
            return netBraces == 0 && previous != null && token.is("{") && !previous.is(TokenType.BRACE_IS_LITERAL);
        }
    }

    private class FirstLevelIterator extends LevelIterator {
        FirstLevelIterator() {
            super(1);
        }
    }

    private class ZerothLevelIterator extends LevelIterator {
        ZerothLevelIterator() {
            super(0);
        }
    }

    @Contract(" -> new")
    @NotNull
    @Override
    public Iterator<Token> iterator() {
        return new TokenIterator();
    }

    /**
     * Get the iterator from a certain point instead of 0.
     * @param i The number of spots from the beginning to start at
     * @return The new iterable
     */
    @NotNull
    @Contract(pure = true)
    public Iterable<Token> iteratorFrom(int i) {
        return () -> new TokenIterator(i);
    }

    /**
     * Get the matching brace of the first token in the list.
     * @return The matching brace
     */
    @NotNull
    public String matchingBrace() {
        assert tokenIs(TokenType.OPEN_BRACE);
        return TokenList.matchingBrace(tokenSequence());
    }

    /**
     * Create an {@link ParserInternalError internal error} from the current
     * context.
     * @param message The message to attach to the error
     * @return The new error
     */
    @NotNull
    public ParserInternalError internalError(String message) {
        return ParserInternalError.of(message, getFirst());
    }

    /**
     * Create a {@link ParserException syntax error} from the current context.
     * @param message The message to attach to the error
     * @return The new error
     */
    @NotNull
    @Contract("_ -> new")
    public ParserException error(String message) {
        return ParserException.of(message, getFirst());
    }

    @NotNull
    public ParserException errorf(String message, Object... args) {
        return ParserException.of(String.format(message, args), getFirst());
    }

    @NotNull
    public ParserException errorWithFirst(String message) {
        return error(message + " " + getFirst());
    }

    @NotNull
    public ParserException errorExpected(String expected) {
        return errorf("Expected %s, got %s", expected, getFirst());
    }

    public void expect(String expected) {
        if (!tokenIs(expected)) {
            throw errorExpected(expected);
        }
        nextToken();
    }

    public void expect(String expected, boolean ignoreNewlines) {
        if (!tokenIs(expected)) {
            throw errorExpected(expected);
        }
        nextToken(ignoreNewlines);
    }

    public void expect(Keyword expected, boolean ignoreNewlines) {
        if (!tokenIs(expected)) {
            throw errorExpected(expected.name);
        }
        nextToken(ignoreNewlines);
    }

    public void expect(TokenType expected, String message, boolean ignoreNewlines) {
        if (!tokenIs(expected)) {
            throw errorExpected(message);
        }
        nextToken(ignoreNewlines);
    }

    @NotNull
    public ParserException defaultError() {
        return errorWithFirst("Unexpected");
    }

    public LineInfo lineInfo() {
        return getFirst().lineInfo;
    }

    public LineInfo lineInfo(int index) {
        return getToken(index).lineInfo;
    }

    /**
     * Get the matching brace of a string.
     * @param brace The brace to match
     * @return The matched brace
     */
    @NotNull
    @Contract(pure = true)
    static String matchingBrace(@NotNull String brace) {
        switch (brace) {
            case "(":
                return ")";
            case "[":
                return "]";
            case "{":
                return "}";
            default:
                throw new RuntimeException("Unknown brace "+brace);
        }
    }
}
