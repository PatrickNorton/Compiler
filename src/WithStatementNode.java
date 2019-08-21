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
}
