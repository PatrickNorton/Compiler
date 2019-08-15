public class ContinueStatementNode implements SimpleFlowNode {
    private TestNode cond;

    public ContinueStatementNode(TestNode cond) {
        this.cond = cond;
    }

    public TestNode getCond() {
        return cond;
    }
}
