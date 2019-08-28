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

    static ImportStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("import");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new ImportStatementNode(imports);
    }
}
