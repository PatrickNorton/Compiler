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
    private StatementBodyNode body;

    @Contract(pure = true)
    public DeferStatementNode(StatementBodyNode body) {
        this.body = body;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    /**
     * Parse a defer statement from a list of tokens.
     * <p>
     *     The syntax for a defer statement is: <code>"defer" {@link
     *     StatementBodyNode}</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DeferStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    public static DeferStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEFER);
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new DeferStatementNode(body);
    }
}
