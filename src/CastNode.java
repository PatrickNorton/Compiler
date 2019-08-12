public class CastNode implements AtomicNode {
    private TypeNode cast;
    private TestNode casted;
    private TestNode post_cast;

    public CastNode(TypeNode cast, TestNode casted, TestNode post_cast) {
        this.cast = cast;
        this.casted = casted;
        this.post_cast = post_cast;
    }

    public TypeNode getCast() {
        return cast;
    }

    public TestNode getCasted() {
        return casted;
    }

    public TestNode getPost_cast() {
        return post_cast;
    }
}
