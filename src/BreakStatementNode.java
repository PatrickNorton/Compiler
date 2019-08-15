public class BreakStatementNode implements SimpleFlowNode {
    private Integer loops;
    private TestNode cond;

    public BreakStatementNode(Integer loops, TestNode cond) {
        this.loops = loops;
        this.cond = cond;
    }

    public Integer getLoops() {
        return loops;
    }

    public TestNode getCond() {
        return cond;
    }
}
