public class WhileStatementNode implements ComplexStatementNode {
    private TestNode cond;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    public WhileStatementNode(TestNode cond, StatementBodyNode body, StatementBodyNode nobreak) {
        this.cond = cond;
        this.body = body;
        this.nobreak = nobreak;
    }

    public TestNode getCond() {
        return cond;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }
}
