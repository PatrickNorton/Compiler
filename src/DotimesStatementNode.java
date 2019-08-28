public class DotimesStatementNode implements FlowStatementNode {
    private TestNode iterations;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    public DotimesStatementNode(TestNode iterations, StatementBodyNode body, StatementBodyNode nobreak) {
        this.iterations = iterations;
        this.body = body;
        this.nobreak = nobreak;
    }

    public TestNode getIterations() {
        return iterations;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }

    static DotimesStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("dotimes");
        tokens.nextToken();
        TestNode iterations = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        tokens.Newline();
        return new DotimesStatementNode(iterations, body, nobreak);
    }
}
