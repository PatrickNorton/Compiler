import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;

public class TokenList implements Iterable<Token> {
    private LinkedList<Token> tokens;

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
