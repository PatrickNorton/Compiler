import java.util.LinkedList;

public class WithStatementNode implements ComplexStatementNode {
    private TestNode[] managed;
    private VariableNode[] vars;
    private StatementBodyNode body;

    public WithStatementNode(TestNode[] managed, VariableNode[] vars, StatementBodyNode body) {
        this.managed = managed;
        this.vars = vars;
        this.body = body;
    }

    public TestNode[] getManaged() {
        return managed;
    }

    public VariableNode[] getVars() {
        return vars;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    static WithStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("with");
        tokens.nextToken();
        LinkedList<TestNode> managed = new LinkedList<>();
        while (!tokens.tokenIs("as")) {
            managed.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
            } else if (!tokens.tokenIs("as")) {
                throw new ParserException("Expected comma or as, got "+tokens.getFirst());
            }
        }
        VariableNode[] vars = VariableNode.parseList(tokens,  false);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        tokens.Newline();
        return new WithStatementNode(managed.toArray(new TestNode[0]), vars, body);
    }
}
