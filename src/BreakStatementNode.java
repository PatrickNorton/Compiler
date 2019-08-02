public class BreakStatementNode implements SimpleFlowNode {
    private Integer loops;

    public BreakStatementNode(Integer loops) {
        this.loops = loops;
    }

    public Integer getLoops() {
        return loops;
    }
}
