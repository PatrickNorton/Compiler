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

    static WhileStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("while");
        tokens.nextToken();
        TestNode cond = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        tokens.Newline();
        return new WhileStatementNode(cond, body, nobreak);
    }
}
