public class BreakStatementNode implements SimpleFlowNode {
    private Integer loops;
    private TestNode cond;

    public BreakStatementNode(Integer loops, TestNode cond) {
        this.loops = loops;
        this.cond = cond;
    }

    public Integer getLoops() {
        return loops;
    }

    public TestNode getCond() {
        return cond;
    }

    static BreakStatementNode parse(TokenList tokens) {  // REFACTORED: BreakStatementNode.parse
        assert tokens.tokenIs("break");
        tokens.nextToken();
        int loops;
        if (tokens.tokenIs(TokenType.NUMBER)) {
            loops = Integer.parseInt(tokens.getFirst().sequence);
            tokens.nextToken();
        } else if (tokens.tokenIs(TokenType.NEWLINE) || tokens.tokenIs("if")) {
            loops = 0;
        } else {
            throw new ParserException("Break statement must not be followed by anything");
        }
        TestNode cond = null;
        if (tokens.tokenIs("if")) {
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        tokens.Newline();
        return new BreakStatementNode(loops, cond);
    }
}
