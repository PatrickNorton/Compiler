// TODO: "defer return" statement
package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing a defer statement.
 *
 * @author Patrick Norton
 */
public class DeferStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private StatementBodyNode body;

    @Contract(pure = true)
    public DeferStatementNode(LineInfo info, StatementBodyNode body) {
        this.lineInfo = info;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    /**
     * Parse a defer statement from a list of tokens.
     * <p>
     *     The syntax for a defer statement is: <code>"defer" ({@link
     *     StatementBodyNode} | {@link ReturnStatementNode})</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DeferStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    public static DeferStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEFER);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        StatementBodyNode body;
        if (tokens.tokenIs(Keyword.RETURN)) {
            body = new StatementBodyNode(ReturnStatementNode.parse(tokens));
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new DeferStatementNode(info, body);
    }

    @Override
    public String toString() {
        return "defer " + body;
    }
}
