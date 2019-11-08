package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a return statement.
 *
 * @author Patrick Norton
 */
public class ReturnStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private TestNode[] returned;
    private TestNode cond;

    /**
     * Construct a new instance of ReturnStatementNode.
     * @param returned The list of tokens that are returned
     * @param cond The condition as to whether or not there is a return
     */
    @Contract(pure = true)
    public ReturnStatementNode(LineInfo lineInfo, TestNode[] returned, TestNode cond) {
        this.lineInfo = lineInfo;
        this.returned = returned;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode[] getReturned() {
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
        TestNode[] returned;
        returned = TestNode.parseListNoTernary(tokens, false);
        TestNode cond = TestNode.parseOnToken(tokens, Keyword.IF, false);
        return new ReturnStatementNode(lineInfo, returned, cond);
    }

    @Override
    public String toString() {
        return "return " + TestNode.toString(returned) + (!cond.isEmpty() ? " if " + cond : "");
    }
}
