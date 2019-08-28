import java.util.LinkedList;

public class YieldStatementNode implements SimpleStatementNode {
    private boolean is_from;
    private TestNode[] yielded;

    public YieldStatementNode(boolean is_from, TestNode... yielded) {
        this.is_from = is_from;
        this.yielded = yielded;
    }

    public TestNode[] getYielded() {
        return yielded;
    }

    public boolean getIs_from() {
        return is_from;
    }

    static YieldStatementNode parse(TokenList tokens) {  // REFACTORED: YieldStatementNode.parse
        assert tokens.tokenIs("yield");
        tokens.nextToken();
        boolean is_from = tokens.tokenIs("from");
        if (is_from) {
            tokens.nextToken();
        }
        LinkedList<TestNode> yields = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE)) {
            yields.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
                continue;
            }
            if (!tokens.tokenIs(TokenType.NEWLINE)) {
                throw new ParserException("Comma must separate yields");
            }
        }
        return new YieldStatementNode(is_from, yields.toArray(new TestNode[0]));
    }
}
