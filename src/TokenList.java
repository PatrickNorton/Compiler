import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;

public class TokenList implements Iterable<Token> {
    private LinkedList<Token> tokens;

    public boolean contains(boolean braces, TokenType... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
        }
    }

    public boolean contains(boolean braces, String... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
        }
    }

    public boolean lineContains(TokenType... question) {
        int netBraces = 0;
        for (Token token : tokens) {
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

    public boolean lineContains(String... question) {
        for (Token token : tokens) {
            if (token.is(TokenType.NEWLINE)) {
                return false;
            }
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    private LinkedList<Token> firstLevel() {
        LinkedList<Token> tokens = new LinkedList<>();
        int netBraces = this.tokenIs(TokenType.OPEN_BRACE) ? 0 : 1;
        for (Token token : this.tokens) {
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

    public boolean braceContains(TokenType... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    public boolean braceContains(String... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    public int sizeOfVariable() {
        return sizeOfVariable(0);
    }

    public int sizeOfVariable(int offset) {
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

    public int sizeOfBrace(int offset) {
        int netBraces = 0;
        int size = offset;
        for (Token token : tokens) {
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

    public void Newline() {
        if (!tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+getFirst());
        }
        nextToken();
    }

    public void passNewlines() {
        while (!isEmpty() && tokenIs(TokenType.NEWLINE)) {
            nextToken(false);
        }
    }

    public TokenList(LinkedList<Token> tokens) {
        this.tokens = tokens;
    }

    public Token getToken(int index) {
        return tokens.get(index);
    }

    public Token getFirst() {
        return tokens.getFirst();
    }

    public boolean tokenIs(TokenType... types) {
        return tokens.getFirst().is(types);
    }

    public boolean tokenIs(String... types) {
        return tokens.getFirst().is(types);
    }

    public boolean tokenIs(int index, TokenType... types) {
        return tokens.get(index).is(types);
    }

    public boolean tokenIs(int index, String... types) {
        return tokens.get(index).is(types);
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public void nextToken() {
        nextToken(false);
    }

    public void nextToken(boolean ignore_newlines) {
        tokens.pop();
        if (ignore_newlines) {
            while (tokens.getFirst().is(TokenType.NEWLINE)) {
                tokens.pop();
            }
        }
    }

    @NotNull
    @Override
    public Iterator<Token> iterator() {
        return tokens.iterator();
    }

    public int size() {
        return tokens.size();
    }
}
