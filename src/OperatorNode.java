public class OperatorNode implements SubTestNode {
    private String operator;
    private TestNode[] operands;

    public OperatorNode(String operator, TestNode... operands) {
        this.operator = operator;
        this.operands = operands;
    }

    public OperatorNode(String operator, TypedArgumentListNode operands) {
        this.operator = operator;
        this.operands = operands.getArgs();
    }

    public OperatorNode(OperatorTypeNode operator, TestNode... operands) {
        this.operator = operator.getName();
        this.operands = operands;
    }

    public String getOperator() {
        return operator;
    }

    public TestNode[] getOperands() {
        return operands;
    }
}
