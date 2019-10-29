package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a continue statement.
 *
 * @author Patrick Norton
 * @see BreakStatementNode
 */
public class ContinueStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private TestNode cond;

    @Contract(pure = true)
    public ContinueStatementNode(LineInfo lineInfo, TestNode cond) {
        this.lineInfo = lineInfo;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Parse a ContinueStatementNode from a list of tokens.
     * <p>
     *     The syntax for a continue statement is: <code>"continue" ["if"
     *     {@link TestNode}</code>. The list of tokens passed must begin with
     *     "continue".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ContinueStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ContinueStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CONTINUE);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode cond = TestNode.parseOnToken(tokens, Keyword.IF, false);
        return new ContinueStatementNode(info, cond);
    }

    @Override
    public String toString() {
        return cond.isEmpty() ? "continue" : "continue " + cond;
    }
}
