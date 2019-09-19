package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a raise statement.
 */
public class RaiseStatementNode implements SimpleStatementNode {
    private TestNode raised;

    /**
     * Create a new instance of Parser.RaiseStatementNode.
     * @param raised The statement to be raised
     */
    @Contract(pure = true)
    public RaiseStatementNode(TestNode raised) {
        this.raised = raised;
    }

    public TestNode getRaised() {
        return raised;
    }

    /**
     * Parse a raise statement from a list of tokens.
     * <p>
     *     The syntax of a raise statement is: <code>"raise" {@link
     *     TestNode}</code>. The token list must begin with the "raise"
     *     keyword.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed Parser.RaiseStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static RaiseStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("raise");
        tokens.nextToken();
        TestNode raised = TestNode.parse(tokens);
        return new RaiseStatementNode(raised);
    }

    @Override
    public String toString() {
        return "raise " + raised;
    }
}
