package Parser;

import org.jetbrains.annotations.NotNull;

public interface InlineableNode extends IndependentNode {
    boolean isInline();
    void setInline(boolean val);

    @NotNull
    static InlineableNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.INLINE);
        tokens.nextToken();
        Token keyword = tokens.getFirst();
        if (!keyword.is(TokenType.KEYWORD)) {
            throw tokens.error("inline must be followed by a keyword, not " + keyword);
        }
        IndependentNode stmt = Keyword.find(keyword).parseLeft(tokens);
        if (!(stmt instanceof InlineableNode)) {
            throw tokens.error("Non-inlineable statement");
        } else {
            ((InlineableNode) stmt).setInline(true);
            return (InlineableNode) stmt;
        }
    }
}
