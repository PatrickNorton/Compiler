public class RaiseStatementNode implements SimpleStatementNode {
    private TestNode raised;

    public RaiseStatementNode(TestNode raised) {
        this.raised = raised;
    }

    public TestNode getRaised() {
        return raised;
    }

    static RaiseStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("raise");
        tokens.nextToken();
        TestNode raised = TestNode.parse(tokens);
        tokens.Newline();
        return new RaiseStatementNode(raised);
    }
}
