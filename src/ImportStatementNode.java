import java.util.Optional;

public class ImportStatementNode implements ImportExportNode {
    private VariableNode[] imports;
    private VariableNode from;

    public ImportStatementNode(VariableNode[] imports, VariableNode from) {
        this.imports = imports;
        this.from = from;
    }

    public ImportStatementNode(VariableNode[] imports) {
        this.imports = imports;
        this.from = null;
    }

    public VariableNode[] getImports() {
        return imports;
    }

    public VariableNode getFrom() {
        return from;
    }
}
