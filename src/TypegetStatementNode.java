public class TypegetStatementNode implements ImportExportNode {
    private DottedVariableNode[] typegets;
    private DottedVariableNode from;

    public TypegetStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this.typegets = imports;
        this.from = from;
    }

    public TypegetStatementNode(DottedVariableNode[] imports) {
        this.typegets = imports;
        this.from = null;
    }

    public DottedVariableNode[] getTypegets() {
        return typegets;
    }

    public DottedVariableNode getFrom() {
        return from;
    }
}
