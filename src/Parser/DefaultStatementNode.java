package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a the default branch of a switch statement.
 *
 * @author Patrick Norton
 */
public class DefaultStatementNode extends CaseStatementNode {
    @Contract(pure = true)
    public DefaultStatementNode(LineInfo lineInfo, StatementBodyNode body, boolean fallthrough) {
        super(lineInfo, new TestNode[0], body, fallthrough);
    }

    @Contract(pure = true)
    public DefaultStatementNode(StatementBodyNode body, boolean fallthrough) {
        this(body.getLineInfo(), body, fallthrough);
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
     * @return The freshly parsed DefaultStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    public static DefaultStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEFAULT);
        tokens.nextToken();
        StatementBodyNode body;
        boolean arrow;
        if (tokens.tokenIs(TokenType.DOUBLE_ARROW)) {
            tokens.nextToken(true);
            body = new StatementBodyNode(TestNode.parseList(tokens, false));
            arrow = true;
        } else {
            body = StatementBodyNode.parse(tokens);
            arrow = false;
        }
        return new DefaultStatementNode(body, arrow);
    }

    @Override
    public String toString() {
        return "default " + (isArrow() ? "=> ..." : getBody().toString());
    }
}
