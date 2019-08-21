public class ExportStatementNode implements ImportExportNode {
    private DottedVariableNode[] exports;

    public ExportStatementNode(DottedVariableNode[] exports) {
        this.exports = exports;
    }

    public DottedVariableNode[] getExports() {
        return exports;
    }
}
