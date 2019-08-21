public class TryStatementNode implements ComplexStatementNode {
    private StatementBodyNode body;
    private StatementBodyNode except;
    private VariableNode[] excepted;
    private VariableNode as_var;
    private StatementBodyNode else_stmt;
    private StatementBodyNode finally_stmt;

    public TryStatementNode(StatementBodyNode body, StatementBodyNode except, VariableNode[] excepted,
                            VariableNode as_var, StatementBodyNode else_stmt, StatementBodyNode finally_stmt) {
        this.body = body;
        this.except = except;
        this.excepted = excepted;
        this.as_var = as_var;
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

    public VariableNode[] getExcepted() {
        return excepted;
    }

    public VariableNode getAs_var() {
        return as_var;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }

    public StatementBodyNode getFinally_stmt() {
        return finally_stmt;
    }
}
