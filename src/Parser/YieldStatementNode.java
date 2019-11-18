package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a yield statement.
 * @author Patrick Norton
 */
public class YieldStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private boolean is_from;
    private TestNode[] yielded;
    private TestNode cond;

    @Contract(pure = true)
    public YieldStatementNode(LineInfo lineInfo, boolean is_from, TestNode[] yielded, TestNode cond) {
        this.lineInfo = lineInfo;
        this.is_from = is_from;
        this.yielded = yielded;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode[] getYielded() {
        return yielded;
    }

    public boolean getIs_from() {
        return is_from;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Given a list of tokens, parse a YieldStatementNode.
     * <p>
     *     The syntax for a yield statement is: <code>"yield" ["from"] {@link
     *     TestNode} *("," {@link TestNode}) [",']</code>. The passed list must
     *     begin with "yield" when passed.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed YieldStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static YieldStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.YIELD);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        boolean is_from = tokens.tokenIs(Keyword.FROM);
        if (is_from) {
            tokens.nextToken();
        }
        TestNode[] yields = TestNode.parseListNoTernary(tokens, false);
        TestNode cond = TestNode.parseOnToken(tokens, Keyword.IF, false);
        return new YieldStatementNode(lineInfo, is_from, yields, cond);
    }

    @Override
    public String toString() {
        return (is_from ? "yield from " : "yield ") + TestNode.toString(yielded);
    }
}
