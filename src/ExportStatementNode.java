public class ExportStatementNode implements ImportExportNode {
    private DottedVariableNode[] exports;

    public ExportStatementNode(DottedVariableNode[] exports) {
        this.exports = exports;
    }

    public DottedVariableNode[] getExports() {
        return exports;
    }

    static ExportStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("export");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty export statements are illegal");
        }
        DottedVariableNode[] exports = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new ExportStatementNode(exports);
    }
}
