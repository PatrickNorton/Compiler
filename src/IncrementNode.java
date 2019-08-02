public class IncrementNode implements SimpleStatementNode {
    private VariableNode variable;

    public IncrementNode(VariableNode variable) {
        this.variable = variable;
    }

    public VariableNode getVariable() {
        return variable;
    }
}
