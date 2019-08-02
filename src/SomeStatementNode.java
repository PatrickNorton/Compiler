public class SomeStatementNode implements SubTestNode {
    private TestNode contained;
    private TestNode container;

    public SomeStatementNode(TestNode contained, TestNode container) {
        this.contained = contained;
        this.container = container;
    }

    public TestNode getContained() {
        return contained;
    }

    public TestNode getContainer() {
        return container;
    }
}
