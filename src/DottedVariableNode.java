public class DottedVariableNode implements NameNode {
    private TestNode preDot;
    private NameNode postDot;

    public DottedVariableNode(TestNode preDot, NameNode postDot) {
        this.preDot = preDot;
        this.postDot = postDot;
    }

    public TestNode getPreDot() {
        return preDot;
    }

    public TestNode getPostDot() {
        return postDot;
    }
}
