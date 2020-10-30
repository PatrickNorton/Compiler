package main.java.parser;

import main.java.util.Pair;

/**
 * The node representing a break statement.
 *
 * @author Patrick Norton
 */
public class BreakStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private int loops;
    private TestNode cond;
    private TestNode as;

    /**
     * Create new BreakStatementNode.
     * @param loops The number of loops to be broken from
     * @param cond The conditional to be tested for
     */

    public BreakStatementNode(LineInfo lineInfo, Integer loops, TestNode cond, TestNode as) {
        this.lineInfo = lineInfo;
        this.loops = loops;
        this.cond = cond;
        this.as = as;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public int getLoops() {
        return loops;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Parse BreakStatementNode from list of tokens.
     * <p>
     *     The break statement consists of three parts: the "break" keyword, the
     *     number of loops broken (optional), and the condition upon which the
     *     break is to occur (optional). The statement is of the form <code>
     *     "break" [number] ["as" {@link TestNode}] ["if" {@link
     *     TestNode}]</code>. The first token in the list parsed must be
     *     "break", otherwise an AssertionError will be thrown.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The parsed break node
     */

    static BreakStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.BREAK);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        int loops;
        if (tokens.tokenIs(TokenType.NUMBER)) {
            loops = Integer.parseInt(tokens.tokenSequence());
            tokens.nextToken();
        } else {
            loops = 1;
        }
        TestNode as, cond;
        if (tokens.tokenIs(Keyword.AS)) {
            tokens.nextToken();
            Pair<TestNode, TestNode> asAndCond = TestNode.parseMaybePostIf(tokens, false);
            as = asAndCond.getKey();
            cond = asAndCond.getValue();
        } else if (tokens.tokenIs(Keyword.IF)) {
            tokens.nextToken();
            cond = TestNode.parse(tokens);
            as = TestNode.empty();
        } else {
            cond = TestNode.empty();
            as = TestNode.empty();
        }
        return new BreakStatementNode(info, loops, cond, as);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("break");
        if (loops > 0) {
            sb.append(" ").append(loops);
        }
        if (!as.isEmpty()) {
            sb.append(" as ").append(as);
        }
        if (!cond.isEmpty()) {
            sb.append(" if ").append(cond);
        }
        return sb.toString();
    }
}
