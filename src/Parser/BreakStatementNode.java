package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing a break statement.
 *
 * @author Patrick Norton
 */
public class BreakStatementNode implements SimpleFlowNode {
    private Integer loops;
    private TestNode cond;

    /**
     * Create new Parser.BreakStatementNode.
     * @param loops The number of loops to be broken from
     * @param cond The conditional to be tested for
     */
    @Contract(pure = true)
    public BreakStatementNode(Integer loops, TestNode cond) {
        this.loops = loops;
        this.cond = cond;
    }

    public Integer getLoops() {
        return loops;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Parse Parser.BreakStatementNode from list of tokens.
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
        tokens.nextToken();
        int loops;
        if (tokens.tokenIs(TokenType.NUMBER)) {
            loops = Integer.parseInt(tokens.getFirst().sequence);
            tokens.nextToken();
        } else if (tokens.tokenIs(TokenType.NEWLINE, "if")) {
            loops = 0;
        } else {
            throw new ParserException("Break statement must not be followed by anything");
        }
        TestNode cond = TestNode.empty();
        if (tokens.tokenIs(Keyword.IF)) {
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        return new BreakStatementNode(loops, cond);
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
