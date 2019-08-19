public class AugmentedAssignmentNode implements AssignStatementNode {
    private OperatorNode operator;
    private NameNode name;
    private TestNode value;

    public AugmentedAssignmentNode(OperatorNode operator, NameNode name, TestNode value) {
        this.operator = operator;
        this.name = name;
        this.value = value;
    }

    public OperatorNode getOperator() {
        return operator;
    }

    public NameNode[] getName() {
        return new NameNode[] {name};
    }

    public TestNode getValue() {
        return value;
    }
}
