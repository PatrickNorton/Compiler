public class ElifStatementNode implements FlowStatementNode {
    private TestNode test;
    private StatementBodyNode body;

    public ElifStatementNode(TestNode test, StatementBodyNode body) {
        this.test = test;
        this.body = body;
    }

    public TestNode getTest() {
        return test;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }
}
