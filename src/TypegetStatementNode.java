public class TypegetStatementNode implements ImportExportNode {
    private VariableNode[] typegets;
    private VariableNode from;

    public TypegetStatementNode(VariableNode[] imports, VariableNode from) {
        this.typegets = imports;
        this.from = from;
    }

    public TypegetStatementNode(VariableNode[] imports) {
        this.typegets = imports;
        this.from = null;
    }

    public VariableNode[] getTypegets() {
        return typegets;
    }

    public VariableNode getFrom() {
        return from;
    }
}
