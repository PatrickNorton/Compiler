public class SomeStatementNode implements SubTestNode {
    private TestNode contained;
    private TestNode container;

    public SomeStatementNode(TestNode contained, TestNode container) {
        this.contained = contained;
        this.container = container;
    }

    public TestNode getContained() {
        return contained;
    }

    public TestNode getContainer() {
        return container;
    }

    static SomeStatementNode parse(TokenList tokens) {  // REFACTORED: SomeStatementNode.parse
        assert tokens.tokenIs("some");
        tokens.nextToken();
        TestNode contained = TestNode.parse(tokens);
        if (!(contained instanceof OperatorNode)) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
        }
        OperatorNode in_stmt = (OperatorNode) contained;
        if (!in_stmt.getOperator().equals("in")) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
        }
        TestNode[] operands = in_stmt.getOperands();
        return new SomeStatementNode(operands[0], operands[1]);
    }
}
