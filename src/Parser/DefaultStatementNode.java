package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a the default branch of a switch statement.
 *
 * @author Patrick Norton
 */
public class DefaultStatementNode implements BaseNode, EmptiableNode {
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

    public boolean hasFallthrough() {
        return fallthrough;
    }

    @Override
    public boolean isEmpty() {
        return body.isEmpty();
    }

    /**
     * Parse a DefaultStatementNode from a list of tokens.
     * <p>
     *     The syntax for a default statement is: <code>"default" ({@link
     *     StatementBodyNode} | (":" *NEWLINE *({@link IndependentNode}
     *     *NEWLINE)))</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DefaultStatementNode
     */
    @NotNull
    public static DefaultStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEFAULT);
        tokens.nextToken();
        return parse(tokens, tokens.tokenIs(1, TokenType.COLON));
    }

    /**
     * Parse a DefaultStatementNode with the given fallthrough from a list of
     * tokens.
     * <p>
     *     The syntax for a default statement is: <code>"default" ({@link
     *     StatementBodyNode} | (":" *NEWLINE *({@link IndependentNode}
     *     *NEWLINE)))</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param fallthrough The fallthrough of the statement
     * @return The freshly parsed DefaultStatementNode
     */
    @NotNull
    @Contract("_, _ -> new")
    public static DefaultStatementNode parse(@NotNull TokenList tokens, boolean fallthrough) {
        assert tokens.tokenIs(Keyword.DEFAULT);
        tokens.nextToken();
        StatementBodyNode body;
        if (fallthrough) {
            if (!tokens.tokenIs(TokenType.COLON)) {
                throw new ParserException("Expected :, got " + tokens.getFirst());
            }
            tokens.nextToken(true);
            body = StatementBodyNode.parseCase(tokens);
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new DefaultStatementNode(body, fallthrough);
    }

    @NotNull
    @Contract("_ -> new")
    public static DefaultStatementNode parseExpression(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEFAULT);
        tokens.nextToken();
        if (!tokens.tokenIs(TokenType.DOUBLE_ARROW)) {
            throw new ParserException("Unexpected " + tokens.getFirst());
        }
        StatementBodyNode body = new StatementBodyNode(TestNode.parse(tokens));
        return new DefaultStatementNode(body, false);
    }
}
