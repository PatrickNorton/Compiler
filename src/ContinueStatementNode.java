public class ContinueStatementNode implements SimpleFlowNode {
    private TestNode cond;

    public ContinueStatementNode(TestNode cond) {
        this.cond = cond;
    }

    public TestNode getCond() {
        return cond;
    }

    static ContinueStatementNode parse(TokenList tokens) {  // REFACTORED: ContinueStatementNode.parse
        assert tokens.tokenIs("continue");
        tokens.nextToken();
        TestNode cond = null;
        if (tokens.tokenIs("if")) {
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        tokens.Newline();
        return new ContinueStatementNode(cond);
    }
}
