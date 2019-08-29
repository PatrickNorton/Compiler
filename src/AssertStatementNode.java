public class AssertStatementNode implements SimpleStatementNode {
    private TestNode assertion;

    public AssertStatementNode(TestNode assertion) {
        this.assertion = assertion;
    }

    public TestNode getAssertion() {
        return assertion;
    }

    static AssertStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("assert");
        tokens.nextToken();
        TestNode assertion = TestNode.parse(tokens);
        tokens.Newline();
        return new AssertStatementNode(assertion);
    }
}
