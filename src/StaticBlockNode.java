public class StaticBlockNode implements ClassStatementNode {
    private BaseNode[] stmts;

    public StaticBlockNode(StatementBodyNode stmts) {
        this.stmts = stmts.getStatements();
    }

    public BaseNode[] getStmts() {
        return this.stmts;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        throw new ParserException("Unexpected descriptor in static block");
    }
}
