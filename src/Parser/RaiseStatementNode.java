package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import util.Pair;

/**
 * The class representing a raise statement.
 */
public class RaiseStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private TestNode raised;
    private TestNode condition;
    private TestNode from;

    /**
     * Create a new instance of RaiseStatementNode.
     * @param raised The statement to be raised
     */
    @Contract(pure = true)
    public RaiseStatementNode(LineInfo lineInfo, TestNode raised, TestNode condition, TestNode from) {
        this.lineInfo = lineInfo;
        this.raised = raised;
        this.condition = condition;
        this.from = from;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getRaised() {
        return raised;
    }

    @Override
    public TestNode getCond() {
        return condition;
    }

    public TestNode getFrom() {
        return from;
    }

    /**
     * Parse a raise statement from a list of tokens.
     * <p>
     *     The syntax of a raise statement is: <code>"raise" {@link
     *     TestNode}</code>. The token list must begin with the "raise"
     *     keyword.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed RaiseStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static RaiseStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.RAISE);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        Pair<TestNode, TestNode> raisedAndCondition = TestNode.parseMaybePostIf(tokens, false);
        TestNode raised = raisedAndCondition.getKey();
        TestNode condition, from;
        if (raisedAndCondition.getValue().isEmpty() && tokens.tokenIs(Keyword.FROM)) {
            tokens.nextToken();
            Pair<TestNode, TestNode> fromAndCondition = TestNode.parseMaybePostIf(tokens, false);
            from = fromAndCondition.getKey();
            condition = fromAndCondition.getValue();
        } else {
            from = TestNode.empty();
            condition = raisedAndCondition.getValue();
        }
        return new RaiseStatementNode(lineInfo, raised, condition, from);
    }

    @Override
    public String toString() {
        return "raise " + raised
                + (from.isEmpty() ? "" : " from " + condition)
                + (condition.isEmpty() ? "" : " if " + condition);
    }
}
