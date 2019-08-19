public class IncrementNode implements SimpleStatementNode {
    private NameNode variable;

    public IncrementNode(NameNode variable) {
        this.variable = variable;
    }

    public NameNode getVariable() {
        return variable;
    }
}
