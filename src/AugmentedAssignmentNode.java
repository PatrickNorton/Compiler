public class AugmentedAssignmentNode implements AssignStatementNode {
    private OperatorNode operator;
    private VariableNode name;
    private TestNode value;

    public AugmentedAssignmentNode(OperatorNode operator, VariableNode name, TestNode value) {
        this.operator = operator;
        this.name = name;
        this.value = value;
    }

    public OperatorNode getOperator() {
        return operator;
    }

    public VariableNode[] getName() {
        return new VariableNode[] {name};
    }

    public TestNode getValue() {
        return value;
    }
}
