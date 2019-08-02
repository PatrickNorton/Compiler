public class StatementBodyNode implements BaseNode {
    private BaseNode[] statements;

    public StatementBodyNode(BaseNode... statements) {
        this.statements = statements;
    }

    public BaseNode[] getStatements() {
        return statements;
    }

    public boolean isEmpty() {
        return statements.length > 0;
    }
}
