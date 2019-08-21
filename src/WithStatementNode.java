public class WithStatementNode implements ComplexStatementNode {
    private TestNode[] managed;
    private DottedVariableNode[] vars;
    private StatementBodyNode body;

    public WithStatementNode(TestNode[] managed, DottedVariableNode[] vars, StatementBodyNode body) {
        this.managed = managed;
        this.vars = vars;
        this.body = body;
    }

    public TestNode[] getManaged() {
        return managed;
    }

    public DottedVariableNode[] getVars() {
        return vars;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }
}
