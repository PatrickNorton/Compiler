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
}
