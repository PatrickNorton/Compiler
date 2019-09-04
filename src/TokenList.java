// TODO? Lazy-loading (incl. use of Scanner.findWithinHorizon)

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;

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
        int netBraces = 0;
        for (Token token : this) {
            if (token.is(TokenType.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(TokenType.CLOSE_BRACE)) {
                netBraces--;
            }
            if (netBraces == 0 && token.is(TokenType.NEWLINE)) {
                return false;
            }
            if (netBraces == 0 && token.is(question)) {
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
        for (Token token : this) {
            if (token.is(TokenType.NEWLINE)) {
                return false;
            }
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The list of tokens within the first brace found
     * @return The list of tokens
     */
    // TODO: Make into an iterator
    private LinkedList<Token> firstLevel() {
        LinkedList<Token> tokens = new LinkedList<>();
        int netBraces = this.tokenIs(TokenType.OPEN_BRACE) ? 0 : 1;
        for (Token token : this) {
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
            if (netBraces == 0) {
                return tokens;
            }
            if (netBraces == 1) {
                tokens.add(token);
            }
        }
        throw new RuntimeException("You shouldn't have ended up here");
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
                    }
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
        for (Token token : this) {
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

    public Token getToken(int index) {
        while (buffer.size() <= index) {
            buffer.add(tokenizer.tokenizeNext());
        }
        return buffer.get(index);
    }

    public Token getFirst() {
        if (buffer.isEmpty()) {
            buffer.add(tokenizer.tokenizeNext());
        }
        return buffer.getFirst();
    }

    public boolean tokenIs(TokenType... types) {
        return getFirst().is(types);
    }

    public boolean tokenIs(String... types) {
        return getFirst().is(types);
    }

    public boolean tokenIs(int index, TokenType... types) {
        return getToken(index).is(types);
    }

    public boolean tokenIs(int index, String... types) {
        return getToken(index).is(types);
    }

    public void nextToken() {
        if (buffer.size() >= 1) {
            buffer.pop();
        } else {
            tokenizer.tokenizeNext();
        }
    }

    public void nextToken(boolean ignore_newlines) {
        nextToken();
        if (ignore_newlines) {
            while (getFirst().is(TokenType.NEWLINE)) {
                nextToken();
            }
        }
    }

    private class TokenIterator implements Iterator<Token> {
        private ListIterator<Token> bufferIterator = buffer.listIterator();
        private Token next = null;

//        private TokenIterator() {
//            buffer();
//        }

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

    @NotNull
    @Override
    public Iterator<Token> iterator() {
        return new TokenIterator();
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
