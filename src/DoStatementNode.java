public class DoStatementNode implements FlowStatementNode {
    public StatementBodyNode body;
    public TestNode conditional;

    public DoStatementNode(StatementBodyNode body, TestNode conditional) {
        this.body = body;
        this.conditional = conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public TestNode getConditional() {
        return conditional;
    }

    static DoStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("do");
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        if (!tokens.tokenIs("while")) {
            throw new ParserException("Do statements must have a corresponding while");
        }
        TestNode conditional = TestNode.parse(tokens);
        tokens.Newline();
        return new DoStatementNode(body, conditional);
    }
}
