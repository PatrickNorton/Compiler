package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing a do-while statement.
 * <p>
 *     This node is interesting in that the conditional comes after the loop,
 *     not before, and therefore ought to be parsed as such.
 * </p>
 */
public class DoStatementNode implements FlowStatementNode {
    private StatementBodyNode body;
    private TestNode conditional;

    /**
     * Instantiate new instance of Parser.DoStatementNode.
     * @param body The body of the loop
     * @param conditional The conditional tested for.
     */
    @Contract(pure = true)
    public DoStatementNode(StatementBodyNode body, TestNode conditional) {
        this.body = body;
        this.conditional = conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public TestNode getConditional() {
        return conditional;
    }

    /**
     * Parse a Parser.DoStatementNode from a list of tokens.
     * <p>
     *     The syntax of a do-while statement is: <code>"do" {@link
     *     StatementBodyNode} "while" {@link TestNode}</code>. The passed
     *     Parser.TokenList must have its first token as "do" in order to parse
     *     correctly.
     * </p>
     * @param tokens The list of tokens to be parsed destructively. Must begin
     *               with a Parser.TokenType.KEYWORD with text "do".
     * @return The newly parsed Parser.DoStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static DoStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("do");
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        if (!tokens.tokenIs("while")) {
            throw new ParserException("Do statements must have a corresponding while");
        }
        TestNode conditional = TestNode.parse(tokens);
        return new DoStatementNode(body, conditional);
    }

    @Override
    public String toString() {
        return "do " + body + " while " + conditional;
    }
}
