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

    static TryStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("try");
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode except = new StatementBodyNode();
        VariableNode[] excepted = new VariableNode[0];
        VariableNode as_var = new VariableNode();
        StatementBodyNode else_stmt = new StatementBodyNode();
        StatementBodyNode finally_stmt = new StatementBodyNode();
        if (tokens.tokenIs("except")) {
            tokens.nextToken();
            excepted = VariableNode.parseList(tokens,  false);
            if (tokens.tokenIs("as")) {
                tokens.nextToken();
                as_var = VariableNode.parse(tokens);
            }
            except = StatementBodyNode.parse(tokens);
            if (tokens.tokenIs("else")) {
                tokens.nextToken();
                else_stmt = StatementBodyNode.parse(tokens);
            }
        }
        if (tokens.tokenIs("finally")) {
            tokens.nextToken();
            finally_stmt = StatementBodyNode.parse(tokens);
        }
        if (except.isEmpty() && finally_stmt.isEmpty()) {
            throw new ParserException("Try statement must either have an except or finally clause");
        }
        tokens.Newline();
        return new TryStatementNode(body, except, excepted, as_var, else_stmt, finally_stmt);
    }
}
