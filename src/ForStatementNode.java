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
}
