package main.java.parser;

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
    private LineInfo lineInfo;
    private StatementBodyNode body;
    private TestNode conditional;

    /**
     * Instantiate new instance of DoStatementNode.
     * @param body The body of the loop
     * @param conditional The conditional tested for.
     */
    @Contract(pure = true)
    public DoStatementNode(LineInfo lineInfo, StatementBodyNode body, TestNode conditional) {
        this.lineInfo = lineInfo;
        this.body = body;
        this.conditional = conditional;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public TestNode getConditional() {
        return conditional;
    }

    /**
     * Parse a DoStatementNode from a list of tokens.
     * <p>
     *     The syntax of a do-while statement is: <code>"do" {@link
     *     StatementBodyNode} "while" {@link TestNode}</code>. The passed
     *     TokenList must have its first token as "do" in order to parse
     *     correctly.
     * </p>
     * @param tokens The list of tokens to be parsed destructively. Must begin
     *               with a TokenType.KEYWORD with text "do".
     * @return The newly parsed DoStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static DoStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DO);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        if (!tokens.tokenIs(Keyword.WHILE)) {
            throw tokens.error("Do statements must have a corresponding while");
        }
        TestNode conditional = TestNode.parse(tokens);
        return new DoStatementNode(info, body, conditional);
    }

    @Override
    public String toString() {
        return "do " + body + " while " + conditional;
    }
}
