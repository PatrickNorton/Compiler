public class AugmentedAssignmentNode implements AssignStatementNode {
    private OperatorNode operator;
    private AtomicNode name;
    private TestNode value;

    public AugmentedAssignmentNode(OperatorNode operator, AtomicNode name, TestNode value) {
        this.operator = operator;
        this.name = name;
        this.value = value;
    }

    public OperatorNode getOperator() {
        return operator;
    }

    public SubTestNode[] getName() {
        return new SubTestNode[] {name};
    }

    public TestNode getValue() {
        return value;
    }
}
