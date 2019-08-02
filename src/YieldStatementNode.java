public class YieldStatementNode implements SimpleStatementNode {
    private boolean is_from;
    private TestNode[] yielded;

    public YieldStatementNode(boolean is_from, TestNode... yielded) {
        this.is_from = is_from;
        this.yielded = yielded;
    }

    public TestNode[] getYielded() {
        return yielded;
    }

    public boolean getIs_from() {
        return is_from;
    }
}
