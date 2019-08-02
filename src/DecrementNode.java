public class DecrementNode implements SimpleStatementNode {
    private VariableNode variable;

    public DecrementNode(VariableNode variable) {
        this.variable = variable;
    }

    public VariableNode getVariable() {
        return variable;
    }
}
