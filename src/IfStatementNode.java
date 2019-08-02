public class IfStatementNode implements FlowStatementNode {
    private TestNode conditional;
    private StatementBodyNode body;
    private ElifStatementNode[] elifs;
    private StatementBodyNode else_stmt;

    public IfStatementNode(TestNode conditional, StatementBodyNode body, ElifStatementNode[] elifs, StatementBodyNode else_stmt) {
        this.conditional = conditional;
        this.body = body;
        this.elifs = elifs;
        this.else_stmt = else_stmt;
    }

    public TestNode getConditional() {
        return conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public ElifStatementNode[] getElifs() {
        return elifs;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }
}
