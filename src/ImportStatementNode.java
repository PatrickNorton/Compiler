public class ImportStatementNode implements ImportExportNode {
    private DottedVariableNode[] imports;
    private DottedVariableNode from;

    public ImportStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this.imports = imports;
        this.from = from;
    }

    public ImportStatementNode(DottedVariableNode[] imports) {
        this.imports = imports;
        this.from = null;
    }

    public DottedVariableNode[] getImports() {
        return imports;
    }

    public DottedVariableNode getFrom() {
        return from;
    }
}
