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

    static OperatorNode parseBoolOp(TokenList tokens, boolean ignore_newline) {
        switch (tokens.getFirst().sequence) {
            case "not":
                tokens.nextToken(ignore_newline);
                return new OperatorNode("not", TestNode.parse(tokens));
            case "and":
            case "or":
            case "xor":
                throw new ParserException(tokens.getFirst()+" must be in between statements");
            default:
                throw new RuntimeException("Unknown boolean operator");
        }
    }

    static OperatorNode parse(TokenList tokens, boolean ignore_newline) {
        assert tokens.tokenIs(TokenType.OPERATOR);
        if (!tokens.tokenIs("-")) {
            throw new ParserException("- is the only unary operator");
        }
        tokens.nextToken(ignore_newline);
        TestNode next = TestNode.parse(tokens, ignore_newline);
        return new OperatorNode("-", next);
    }
}
