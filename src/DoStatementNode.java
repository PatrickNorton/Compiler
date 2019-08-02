public class DoStatementNode implements FlowStatementNode {
    public StatementBodyNode body;
    public TestNode conditional;

    public DoStatementNode(StatementBodyNode body, TestNode conditional) {
        this.body = body;
        this.conditional = conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public TestNode getConditional() {
        return conditional;
    }
}
