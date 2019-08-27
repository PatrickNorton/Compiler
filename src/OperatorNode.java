public class OperatorNode implements SubTestNode {
    private OperatorTypeNode operator;
    private TestNode[] operands;

    public OperatorNode(String operator, TestNode... operands) {
        this.operator = OperatorTypeNode.find_op(operator);
        this.operands = operands;
    }

    public OperatorNode(String operator, TypedArgumentListNode operands) {
        this.operator = OperatorTypeNode.find_op(operator);
        this.operands = operands.getArgs();
    }

    public OperatorNode(OperatorTypeNode operator, TestNode... operands) {
        this.operator = operator;
        this.operands = operands;
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }

    public TestNode[] getOperands() {
        return operands;
    }
}
