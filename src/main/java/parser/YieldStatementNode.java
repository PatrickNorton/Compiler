package main.java.parser;

import main.java.util.Pair;

/**
 * The class representing a yield statement.
 * @author Patrick Norton
 */
public class YieldStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private boolean isFrom;
    private TestListNode yielded;
    private TestNode cond;

    public YieldStatementNode(LineInfo lineInfo, boolean isFrom, TestListNode yielded, TestNode cond) {
        this.lineInfo = lineInfo;
        this.isFrom = isFrom;
        this.yielded = yielded;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestListNode getYielded() {
        return yielded;
    }

    public boolean isFrom() {
        return isFrom;
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

    static YieldStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.YIELD);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        boolean isFrom = tokens.tokenIs(Keyword.FROM);
        if (isFrom) {
            tokens.nextToken();
        }
        Pair<TestListNode, TestNode> loopedAndCondition = TestListNode.parsePostIf(tokens, false);
        TestListNode yields = loopedAndCondition.getKey();
        TestNode cond = loopedAndCondition.getValue();
        return new YieldStatementNode(lineInfo, isFrom, yields, cond);
    }

    @Override
    public String toString() {
        return (isFrom ? "yield from " : "yield ") + yielded;
    }
}
