package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing a break statement.
 *
 * @author Patrick Norton
 */
public class BreakStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private Integer loops;
    private TestNode cond;

    /**
     * Create new BreakStatementNode.
     * @param loops The number of loops to be broken from
     * @param cond The conditional to be tested for
     */
    @Contract(pure = true)
    public BreakStatementNode(LineInfo lineInfo, Integer loops, TestNode cond) {
        this.lineInfo = lineInfo;
        this.loops = loops;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public Integer getLoops() {
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
     *     "break" [number] ["if" {@link TestNode}]</code>. The first token in
     *     the list parsed must be "break", otherwise an AssertionError will be
     *     thrown.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The parsed break node
     */
    @NotNull
    @Contract("_ -> new")
    static BreakStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.BREAK);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        int loops;
        if (tokens.tokenIs(TokenType.NUMBER)) {
            loops = Integer.parseInt(tokens.tokenSequence());
            tokens.nextToken();
        } else if (tokens.tokenIs(TokenType.NEWLINE, "if")) {
            loops = 0;
        } else {
            throw tokens.error("Break statement must not be followed by anything");
        }
        TestNode cond = TestNode.parseOnToken(tokens, Keyword.IF, false);
        return new BreakStatementNode(info, loops, cond);
    }

    @Override
    public String toString() {
        String string = "break";
        if (loops > 0) {
            string += " " + loops;
        }
        if (!cond.isEmpty()) {
            string += " if " + cond;
        }
        return string;
    }
}
