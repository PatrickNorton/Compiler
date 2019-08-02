public class DeleteStatementNode implements SimpleStatementNode {
    private TestNode deleted;

    public DeleteStatementNode(TestNode deleted) {
        this.deleted = deleted;
    }

    public TestNode getDeleted() {
        return deleted;
    }
}
