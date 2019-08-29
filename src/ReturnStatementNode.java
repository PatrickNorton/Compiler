public class ReturnStatementNode implements SimpleFlowNode {
    private TestNode[] returned;
    private TestNode cond;

    public ReturnStatementNode(TestNode[] returned, TestNode cond) {
        this.returned = returned;
        this.cond = cond;
    }

    public TestNode[] getReturned() {
        return returned;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    static ReturnStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("return");
        tokens.nextToken();
        boolean is_conditional = false;
        if (tokens.tokenIs("(") && tokens.getToken(tokens.sizeOfBrace(0) + 1).is("if")
                && !tokens.lineContains("else")) {
            tokens.nextToken();
            is_conditional = true;
        }
        TestNode[] returned;
        if (!tokens.tokenIs(TokenType.NEWLINE) && !tokens.tokenIs("if")) {
            returned = TestNode.parseList(tokens, false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = null;
        if (is_conditional) {
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Expected ), got " + tokens.getFirst());
            }
            cond = TestNode.parse(tokens);
        }
        tokens.Newline();
        return new ReturnStatementNode(returned, cond);
    }
}
