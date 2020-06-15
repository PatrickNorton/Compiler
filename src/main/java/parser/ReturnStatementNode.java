package main.java.parser;

import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a return statement.
 *
 * @author Patrick Norton
 */
public class ReturnStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private TestListNode returned;
    private TestNode cond;

    /**
     * Construct a new instance of ReturnStatementNode.
     * @param returned The list of tokens that are returned
     * @param cond The condition as to whether or not there is a return
     */
    @Contract(pure = true)
    public ReturnStatementNode(LineInfo lineInfo, TestListNode returned, TestNode cond) {
        this.lineInfo = lineInfo;
        this.returned = returned;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestListNode getReturned() {
        return returned;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Parse a new return statement from a list of tokens.
     * <p>
     *     The syntax for a return statement is: <code>"return" [{@link
     *     TestNode} *("," {@link TestNode}) [","]] ["if" {@link
     *     TestNode}]</code>. The list of tokens must begin with "return".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed ReturnStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ReturnStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.RETURN);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        Pair<TestListNode, TestNode> loopedAndCondition = TestListNode.parsePostIf(tokens, false);
        TestListNode returned = loopedAndCondition.getKey();
        TestNode cond = loopedAndCondition.getValue();
        return new ReturnStatementNode(lineInfo, returned, cond == null ? TestNode.empty() : cond);
    }

    @Override
    public String toString() {
        return "return " + returned + (!cond.isEmpty() ? " if " + cond : "");
    }
}
