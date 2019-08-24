public class DottedVariableNode implements NameNode {
    private TestNode preDot;
    private NameNode[] postDots;

    public DottedVariableNode(TestNode preDot, NameNode... postDot) {
        this.preDot = preDot;
        this.postDots = postDot;
    }

    public DottedVariableNode() {
        this.preDot = new VariableNode();
        this.postDots = new NameNode[0];
    }

    public TestNode getPreDot() {
        return preDot;
    }

    public TestNode[] getPostDots() {
        return postDots;
    }

    public boolean isEmpty() {
        return (preDot instanceof VariableNode) && ((VariableNode) preDot).isEmpty();
    }
}
