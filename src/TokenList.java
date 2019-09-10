import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * The list of tokens.
 * @author Patrick Norton
 */
public class TokenList implements Iterable<Token> {
    private LinkedList<Token> buffer;
    private Tokenizer tokenizer;

    @Contract(pure = true)
    public TokenList(LinkedList<Token> buffer) {
        this.buffer = buffer;
        this.tokenizer = new Tokenizer("");
    }

    @Contract(pure = true)
    public TokenList(Tokenizer tokenizer) {
        this.buffer = new LinkedList<>();
        this.tokenizer = tokenizer;
    }

    /**
     * Test if either a brace or a line contains something of the types.
     * @param braces Whether or not to check within the brace or the line
     * @param question The questions to test for
     * @return Whether or not the token was found
     */
    boolean contains(boolean braces, TokenType... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
        }
    }

    /**
     * Test if either a brace or a line contains something of the types.
     * @param braces Whether or not to check within the brace or the line
     * @param question The questions to test for
     * @return Whether or not the token was found
     */
    boolean contains(boolean braces, String... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
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

    @NotNull
    @Contract(pure = true)
    private Iterable<Token> lineIterator() {
        return LineIterator::new;
    }

    private class LineIterator implements Iterator<Token> {
        private int netBraces = 0;
        private Iterator<Token> iterator = TokenList.this.iterator();
        private Token next = iterator.next();

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Token next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Token toReturn;
            do {
                toReturn = next;
                if (!iterator.hasNext()) {
                    next = null;
                    break;
                } else {
                    next = iterator.next();
                }
                adjustBraces(toReturn);
            } while (next != null && netBraces > 0);
            if (next == null && netBraces > 0) {
                throw new ParserException("Unmatched braces");
            }
            return toReturn;
        }

        private void adjustBraces(@NotNull Token token) {
            switch (token.token) {
                case OPEN_BRACE:
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    netBraces--;
                    break;
                case EPSILON:
                    if (netBraces > 0) {
                        throw new ParserException("Unmatched brace");
                    }
            }
            if (next == null) {
                return;
            }
            if (next.is("{") && netBraces == 0 &&
                        token.is(TokenType.NAME, TokenType.ELLIPSIS, TokenType.STRING,
                                TokenType.NUMBER, TokenType.OPERATOR_SP, TokenType.CLOSE_BRACE)) {
                next = null;
            } else if (token.is(TokenType.NEWLINE) && netBraces == 0) {
                next = null;
            } else if (netBraces < 0) {
                next = null;
            }
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
        assert tokenIs(offset, TokenType.NAME);
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
                        throw new ParserException("Unmatched brace");
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
        if (!tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+getFirst());
        }
        nextToken();
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
        while (buffer.size() <= index) {
            buffer.add(tokenizer.tokenizeNext());
        }
        return buffer.get(index);
    }

    /**
     * Get the first token in the queue.
     * @return The first token
     */
    public Token getFirst() {
        if (buffer.isEmpty()) {
            buffer.add(tokenizer.tokenizeNext());
        }
        return buffer.getFirst();
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
     * Test if the first token is one of a series of values.
     * @param types The values to check against
     * @return Whether or not the token is of that value
     */
    public boolean tokenIs(String... types) {
        return getFirst().is(types);
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
     * Test if the token at the index is one of a certain set of values.
     * @param index The index to check for value
     * @param types The values to check against
     * @return Whether the token is of those values
     */
    public boolean tokenIs(int index, String... types) {
        return getToken(index).is(types);
    }

    public boolean tokenIs(TokenType type1, String type2) {
        return getFirst().is(type1) || getFirst().is(type2);
    }

    /**
     * Pop the first token and move on.
     */
    public void nextToken() {
        if (buffer.size() >= 1) {
            buffer.pop();
        } else {
            tokenizer.tokenizeNext();
        }
    }

    /**
     * Pop the first token and move on, possibly ignoring newlines.
     * @param ignore_newlines Whether or not to ignore newlines
     */
    public void nextToken(boolean ignore_newlines) {
        nextToken();
        if (ignore_newlines) {
            while (getFirst().is(TokenType.NEWLINE)) {
                nextToken();
            }
        }
    }

    private class TokenIterator implements Iterator<Token> {
        private ListIterator<Token> bufferIterator;
        private Token next = null;

        TokenIterator() {
            bufferIterator = buffer.listIterator();
        }

        TokenIterator(int i) {
            TokenList.this.getToken(i);  // Ensure the buffer is large enough
            bufferIterator = buffer.listIterator(i);
        }

        @Override
        public boolean hasNext() {
            if (bufferIterator.hasNext()) {
                return true;
            }
            if (next == null) {
                buffer();
            }
            return !next.is(TokenType.EPSILON);
        }

        @Override
        public Token next() {
            if (!bufferIterator.hasNext()) {
                buffer();
            }
            return bufferIterator.next();
        }

        private void buffer() {
            next = tokenizer.tokenizeNext();
            bufferIterator.add(next);
            bufferIterator.previous();
        }
    }

    private class FirstLevelIterator implements Iterator<Token> {
        private int netBraces = 0;
        private boolean beginning = true;
        private Iterator<Token> iterator = TokenList.this.iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext() && (beginning || netBraces > 0);
        }

        @Override
        public Token next() {
            beginning = false;
            Token next;
            do {
                next = iterator.next();
                adjustBraces(next);
            } while (netBraces > 1);
            return next;
        }

        private void adjustBraces(@NotNull Token token) {
            switch (token.token) {
                case OPEN_BRACE:
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    netBraces--;
                    break;
                case EPSILON:
                    throw new ParserException("Unmatched brace");
            }
        }
    }

    @NotNull
    @Override
    public Iterator<Token> iterator() {
        return new TokenIterator();
    }

    public Iterable<Token> iteratorFrom(int i) {
        return () -> new TokenIterator(i);
    }

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
