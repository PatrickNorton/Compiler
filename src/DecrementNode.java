public class DecrementNode implements SimpleStatementNode {
    private NameNode variable;

    public DecrementNode(NameNode variable) {
        this.variable = variable;
    }

    public NameNode getVariable() {
        return variable;
    }
}
