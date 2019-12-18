package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class RangeLiteralNode implements TestNode, PostDottableNode {
    private LineInfo lineInfo;
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
    public RangeLiteralNode(LineInfo lineInfo, TestNode start, TestNode end, TestNode step) {
        this.lineInfo = lineInfo;
        this.start = start;
        this.end = end;
        this.step = step;
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

    @NotNull
    public static RangeLiteralNode parse(TokenList tokens) {
        return fromSlice(SliceNode.parse(tokens));
    }

    @NotNull
    @Contract("_ -> new")
    public static RangeLiteralNode fromSlice(@NotNull SliceNode node) {
        return new RangeLiteralNode(node.getLineInfo(), node.getStart(), node.getEnd(), node.getStep());
    }

    @Override
    public String toString() {
        return String.format("[%s:%s%s]", start, end, !step.isEmpty() ? ":" + step : "");
    }
}
