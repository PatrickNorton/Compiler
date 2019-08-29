public class DeleteStatementNode implements SimpleStatementNode {
    private TestNode deleted;

    public DeleteStatementNode(TestNode deleted) {
        this.deleted = deleted;
    }

    public TestNode getDeleted() {
        return deleted;
    }

    static DeleteStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("del");
        tokens.nextToken();
        TestNode deletion = TestNode.parse(tokens);
        tokens.Newline();
        return new DeleteStatementNode(deletion);
    }
}
