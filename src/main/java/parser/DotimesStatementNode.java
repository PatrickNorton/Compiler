package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a dotimes statement.
 * <p>
 *     Dotimes statements <i>may</i> take a variable number of loops iterated,
 *     and do not have to simply take an integer literal, unlike {@link
 *     BreakStatementNode}.
 * </p>
 * @author Patrick Norton
 */
public class DotimesStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private TestNode iterations;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    /**
     * Create a new instance of DotimesStatementNode.
     * @param iterations The number of iterations to be taken
     * @param body The body of the node
     * @param nobreak The nobreak statement
     */
    @Contract(pure = true)
    public DotimesStatementNode(LineInfo lineInfo, TestNode iterations, StatementBodyNode body, StatementBodyNode nobreak) {
        this.lineInfo = lineInfo;
        this.iterations = iterations;
        this.body = body;
        this.nobreak = nobreak;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getIterations() {
        return iterations;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }

    /**
     * Parse a DotimesStatementNode from a list of tokens.
     * <p>
     *     The syntax of a DotimesStatementNode is as follows: <code>"dotimes"
     *     {@link TestNode} {@link StatementBodyNode} ["nobreak" {@link
     *     StatementBodyNode}]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DotimesStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static DotimesStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.DOTIMES);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode iterations = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        return new DotimesStatementNode(info, iterations, body, nobreak);
    }

    @Override
    public String toString() {
        return "dotimes " + iterations + " " + body;
    }
}
