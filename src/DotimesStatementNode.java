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
    public DotimesStatementNode(TestNode iterations, StatementBodyNode body, StatementBodyNode nobreak) {
        this.iterations = iterations;
        this.body = body;
        this.nobreak = nobreak;
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
     * @param tokens
     * @return
     */
    @NotNull
    @Contract("_ -> new")
    static DotimesStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("dotimes");
        tokens.nextToken();
        TestNode iterations = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        tokens.Newline();
        return new DotimesStatementNode(iterations, body, nobreak);
    }
}
