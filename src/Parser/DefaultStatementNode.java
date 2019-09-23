package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class DefaultStatementNode implements BaseNode {
    private StatementBodyNode body;
    private boolean fallthrough;

    @Contract(pure = true)
    public DefaultStatementNode(StatementBodyNode body, boolean fallthrough) {
        this.body = body;
        this.fallthrough = fallthrough;
    }

    @Contract(pure = true)
    public DefaultStatementNode(boolean fallthrough) {
        this(new StatementBodyNode(), fallthrough);
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public boolean isFallthrough() {
        return fallthrough;
    }

    @NotNull
    public static DefaultStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("default");
        tokens.nextToken();
        return parse(tokens, tokens.tokenIs(1, TokenType.COLON));
    }

    @NotNull
    @Contract("_, _ -> new")
    public static DefaultStatementNode parse(@NotNull TokenList tokens, boolean fallthrough) {
        assert tokens.tokenIs("default");
        tokens.nextToken();
        StatementBodyNode body;
        if (fallthrough) {
            if (!tokens.tokenIs(TokenType.COLON)) {
                throw new ParserException("Expected :, got " + tokens.getFirst());
            }
            tokens.nextToken(true);
            body = StatementBodyNode.parseSwitch(tokens);
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new DefaultStatementNode(body, fallthrough);
    }
}