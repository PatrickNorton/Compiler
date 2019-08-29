import java.util.Optional;

public class ForStatementNode implements FlowStatementNode {
    private TypedVariableNode[] vars;
    private TestNode[] iterables;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    public ForStatementNode(TypedVariableNode[] vars, TestNode[] iterables, StatementBodyNode body, StatementBodyNode nobreak) {
        this.vars = vars;
        this.iterables = iterables;
        this.body = body;
        this.nobreak = nobreak;
    }

    public TypedVariableNode[] getVars() {
        return vars;
    }

    public TestNode[] getIterables() {
        return iterables;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }

    static ForStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("for");
        tokens.nextToken();
        TypedVariableNode[] vars = TypedVariableNode.parseForVars(tokens);
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Expected in, got "+tokens.getFirst());
        }
        tokens.nextToken();
        TestNode[] iterables = TestNode.parseForIterables(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        tokens.Newline();
        return new ForStatementNode(vars, iterables, body, nobreak);
    }
}
