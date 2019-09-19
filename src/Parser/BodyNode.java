package Parser;

public interface BodyNode extends BaseNode {
    IndependentNode[] getStatements();
    boolean isEmpty();
}
