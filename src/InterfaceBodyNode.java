public class InterfaceBodyNode extends StatementBodyNode {
    private InterfaceStatementNode[] statements;

    public InterfaceBodyNode(InterfaceStatementNode... statements) {
        this.statements = statements;
    }

    @Override
    public InterfaceStatementNode[] getStatements() {
        return statements;
    }
}
