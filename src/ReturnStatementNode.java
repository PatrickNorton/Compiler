public class ReturnStatementNode implements SimpleFlowNode {
    private TestNode[] returned;
    private TestNode cond;

    public ReturnStatementNode(TestNode[] returned, TestNode cond) {
        this.returned = returned;
        this.cond = cond;
    }

    public TestNode[] getReturned() {
        return returned;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }
}
