public class TernaryNode implements TestNode {
    private SubTestNode if_true;
    private SubTestNode statement;
    private TestNode if_false;

    public TernaryNode(SubTestNode if_true, SubTestNode statement, TestNode if_false) {
        this.if_true = if_true;
        this.statement = statement;
        this.if_false = if_false;
    }

    public SubTestNode getIf_true() {
        return if_true;
    }

    public SubTestNode getStatement() {
        return statement;
    }

    public TestNode getIf_false() {
        return if_false;
    }
}
