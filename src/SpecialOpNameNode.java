public class SpecialOpNameNode implements NameNode {
    private OperatorTypeNode operator;

    public SpecialOpNameNode(OperatorTypeNode operator) {
        this.operator = operator;
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }
}
