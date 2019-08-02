public class AssertStatementNode implements SimpleStatementNode {
    private TestNode assertion;

    public AssertStatementNode(TestNode assertion) {
        this.assertion = assertion;
    }

    public TestNode getAssertion() {
        return assertion;
    }
}
