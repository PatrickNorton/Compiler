package Parser;

public interface BodyNode extends BaseNode, EmptiableNode {
    IndependentNode[] getStatements();
}
