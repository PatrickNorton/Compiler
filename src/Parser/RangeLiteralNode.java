package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class RangeLiteralNode implements TestNode {
    private TestNode start;
    private TestNode end;
    private TestNode step;

    /**
     * Construct a new instance of RangeLiteralNode.
     * @param start The start of the slice
     * @param end The end of the slice
     * @param step The step amount
     */
    @Contract(pure = true)
    public RangeLiteralNode(TestNode start, TestNode end, TestNode step) {
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

    @NotNull
    public static RangeLiteralNode parse(TokenList tokens) {
        return fromSlice(SliceNode.parse(tokens));
    }

    @NotNull
    @Contract("_ -> new")
    public static RangeLiteralNode fromSlice(@NotNull SliceNode node) {
        return new RangeLiteralNode(node.getStart(), node.getEnd(), node.getStep());
    }

    @Override
    public String toString() {
        return "[" + start + ":" + end + (step.isEmpty() ? "" : ":" + step) + "]";
    }
}
