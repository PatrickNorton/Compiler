package Parser;

public interface BodyNode extends BaseNode, Iterable<IndependentNode> {
    IndependentNode[] getStatements();
    boolean isEmpty();
}
