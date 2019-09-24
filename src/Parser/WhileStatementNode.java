package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a while statement.
 * @author Patrick Norton
 * @see DoStatementNode
 */
public class WhileStatementNode implements FlowStatementNode {
    private TestNode cond;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    @Contract(pure = true)
    public WhileStatementNode(TestNode cond, StatementBodyNode body, StatementBodyNode nobreak) {
        this.cond = cond;
        this.body = body;
        this.nobreak = nobreak;
    }

    public TestNode getCond() {
        return cond;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }

    /**
     * Parse a Parser.WhileStatementNode from a list of tokens.
     * <p>
     *     The syntax for a while statement is: <code>"while" {@link TestNode}
     *     {@link StatementBodyNode} ["nobreak" {@link
     *     StatementBodyNode}</code>. The token list must begin with "while" in
     *     order to be valid.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed while statement
     */
    @NotNull
    @Contract("_ -> new")
    static WhileStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("while");
        tokens.nextToken();
        TestNode cond = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        return new WhileStatementNode(cond, body, nobreak);
    }

    @Override
    public String toString() {
        return "while " + cond + " " + body;
    }
}
