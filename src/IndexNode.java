public class IndexNode implements NameNode {
    private NameNode var;
    private TestNode[] indices;

    public IndexNode(NameNode var, TestNode... indices) {
        this.var = var;
        this.indices = indices;
    }

    public NameNode getVar() {
        return var;
    }

    public TestNode[] getIndices() {
        return indices;
    }
}
