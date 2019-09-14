import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class representing a slice an in index.
 * @author Patrick Norton
 * @see IndexNode
 */
public class SliceNode implements SubTestNode {
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
    public SliceNode(TestNode start, TestNode end, TestNode step) {
        this.start = start;
        this.end = end;
        this.step = step;
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
        tokens.nextToken(true);
        TestNode start;
        if (tokens.tokenIs(":")) {
            start = null;
        } else {
            start = TestNode.parse(tokens, true);
        }
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(start, null, null);
        }
        TestNode end = sliceTest(tokens);
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(start, end, null);
        }
        TestNode step = sliceTest(tokens);
        if (!tokens.tokenIs("]")) {
            throw new ParserException("Expected ], got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new SliceNode(start, end, step);
    }

    /**
     * Parse a specific piece of a slice.
     * @param tokens The list of tokens to be parsed destructively
     * @return The parsed TestNode
     */
    @Nullable
    private static TestNode sliceTest(@NotNull TokenList tokens) {
        if (!tokens.tokenIs(":")) {
            throw new ParserException("Expected :, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        if (tokens.tokenIs(":", "]")) {
            return null;
        } else {
            return TestNode.parse(tokens, true);
        }
    }

    @Override
    public String toString() {
        return (start != null ? start : "") + ":" + (end != null ? end : "") + (step != null ? ":" + step : "");
    }
}
