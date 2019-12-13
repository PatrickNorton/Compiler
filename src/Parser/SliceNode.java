package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a slice an in index.
 * @author Patrick Norton
 * @see IndexNode
 */
public class SliceNode implements SubTestNode {
    private LineInfo lineInfo;
    private TestNode start;
    private TestNode end;
    private TestNode step;

    /**
     * Construct a new instance of SliceNode
     * @param start The start of the slice
     * @param end The end of the slice
     * @param step The step amount
     */
    @Contract(pure = true)
    public SliceNode(LineInfo lineInfo, TestNode start, TestNode end, TestNode step) {
        this.lineInfo = lineInfo;
        this.start = start;
        this.end = end;
        this.step = step;
    }

    /**
     * Construct a new instance of SliceNode
     * @param start The start of the slice
     * @param end The end of the slice
     */
    public SliceNode(LineInfo lineInfo, TestNode start, TestNode end) {
        this(lineInfo, start, end, TestNode.empty());
    }

    /**
     * Construct a new instance of SliceNode
     * @param start The start of the slice
     */
    public SliceNode(LineInfo lineInfo, TestNode start) {
        this(lineInfo, start, TestNode.empty(), TestNode.empty());
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getStart() {
        return start;
    }

    public TestNode getEnd() {
        return end;
    }

    public TestNode getStep() {
        return step;
    }

    /**
     * Parse a new instance of SliceNode.
     * <p>
     *     The syntax for a slice is: <code>[{@link TestNode}] ":" [{@link
     *     TestNode}] [":" [{@link TestNode}]]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed node
     */
    @NotNull
    @Contract("_ -> new")
    static SliceNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("[");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        TestNode start;
        if (tokens.tokenIs(TokenType.COLON)) {
            start = TestNode.empty();
        } else {
            start = TestNode.parse(tokens, true);
        }
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(info, start);
        }
        TestNode end = sliceTest(tokens);
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(info, start, end);
        }
        TestNode step = sliceTest(tokens);
        tokens.expect("]");
        return new SliceNode(info, start, end, step);
    }

    /**
     * Parse a specific piece of a slice.
     * @param tokens The list of tokens to be parsed destructively
     * @return The parsed TestNode
     */
    @NotNull
    private static TestNode sliceTest(@NotNull TokenList tokens) {
        tokens.expect(TokenType.COLON, ":", true);
        if (tokens.tokenIs(TokenType.COLON, "]")) {
            return TestNode.empty();
        } else {
            return TestNode.parse(tokens, true);
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s%s", start, end, !step.isEmpty() ? ":" + step : "");
    }
}
