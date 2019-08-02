public class RaiseStatementNode implements SimpleStatementNode {
    private TestNode raised;

    public RaiseStatementNode(TestNode raised) {
        this.raised = raised;
    }

    public TestNode getRaised() {
        return raised;
    }
}
