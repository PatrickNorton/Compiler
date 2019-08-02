public class ExportStatementNode implements ImportExportNode {
    private VariableNode[] exports;

    public ExportStatementNode(VariableNode[] exports) {
        this.exports = exports;
    }

    public VariableNode[] getExports() {
        return exports;
    }
}
