import java.util.LinkedList;

public class IfStatementNode implements FlowStatementNode {
    private TestNode conditional;
    private StatementBodyNode body;
    private ElifStatementNode[] elifs;
    private StatementBodyNode else_stmt;

    public IfStatementNode(TestNode conditional, StatementBodyNode body, ElifStatementNode[] elifs, StatementBodyNode else_stmt) {
        this.conditional = conditional;
        this.body = body;
        this.elifs = elifs;
        this.else_stmt = else_stmt;
    }

    public TestNode getConditional() {
        return conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public ElifStatementNode[] getElifs() {
        return elifs;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }

    static IfStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("if");
        tokens.nextToken();
        TestNode test = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        LinkedList<ElifStatementNode> elifs = new LinkedList<>();
        while (tokens.tokenIs("elif")) {
            tokens.nextToken();
            TestNode elif_test = TestNode.parse(tokens);
            StatementBodyNode elif_body = StatementBodyNode.parse(tokens);
            elifs.add(new ElifStatementNode(elif_test, elif_body));
        }
        StatementBodyNode else_stmt = new StatementBodyNode();
        if (tokens.tokenIs("else")) {
            tokens.nextToken();
            else_stmt = StatementBodyNode.parse(tokens);
        }
        tokens.Newline();
        return new IfStatementNode(test, body, elifs.toArray(new ElifStatementNode[0]), else_stmt);
    }
}
