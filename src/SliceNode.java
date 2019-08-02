public class SliceNode implements SubTestNode {
    private TestNode start;
    private TestNode end;
    private TestNode step;

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
}
