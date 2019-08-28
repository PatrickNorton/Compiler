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

    static TypegetStatementNode parse(TokenList tokens) {
        DottedVariableNode from = new DottedVariableNode();
        if (tokens.tokenIs("from")) {
            tokens.nextToken();
            from = DottedVariableNode.parse(tokens);
        }
        assert tokens.tokenIs("typeget");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new TypegetStatementNode(typegets, from);
    }
}
