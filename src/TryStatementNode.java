public class TryStatementNode implements ComplexStatementNode {
    private StatementBodyNode body;
    private StatementBodyNode except;
    private StatementBodyNode else_stmt;
    private StatementBodyNode finally_stmt;

    public TryStatementNode(StatementBodyNode body, StatementBodyNode except, StatementBodyNode else_stmt, StatementBodyNode finally_stmt) {
        this.body = body;
        this.except = except;
        this.else_stmt = else_stmt;
        this.finally_stmt = finally_stmt;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getExcept() {
        return except;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }

    public StatementBodyNode getFinally_stmt() {
        return finally_stmt;
    }
}
