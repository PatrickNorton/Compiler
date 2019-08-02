public class ClassBodyNode extends StatementBodyNode {
    private ClassStatementNode[] statements;

    public ClassBodyNode(ClassStatementNode... statements) {
        // super(statements);
        this.statements = statements;
    }

    public ClassStatementNode[] getStatements() {
        return statements;
    }
}
