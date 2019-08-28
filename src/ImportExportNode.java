public interface ImportExportNode extends SimpleStatementNode {
    static ImportExportNode parse(TokenList tokens) {  // TODO: Change to parse all import/export nodes
        assert tokens.tokenIs("from");
        if (tokens.lineContains("import")) {
            return ImportStatementNode.parse(tokens);
        } else if (tokens.lineContains("export")) {
            return ExportStatementNode.parse(tokens);
        } else if (tokens.lineContains("typeget")) {
            return TypegetStatementNode.parse(tokens);
        } else {
            throw new ParserException("from does not begin a statement");
        }
    }
}
