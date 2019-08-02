public class ReturnStatementNode implements SimpleFlowNode {
    private TestNode[] returned;

    public ReturnStatementNode(TestNode[] returned) {
        this.returned = returned;
    }

    public TestNode[] getReturned() {
        return returned;
    }
}
