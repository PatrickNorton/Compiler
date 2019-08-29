public class TypedefStatementNode implements SimpleStatementNode {
    private TypeNode name;
    private TypeNode type;

    public TypedefStatementNode(TypeNode name, TypeNode type) {
        this.name = name;
        this.type = type;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    static TypedefStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs("typedef");
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        assert tokens.tokenIs("as");
        tokens.nextToken();
        TypeNode type = TypeNode.parse(tokens);
        tokens.Newline();
        return new TypedefStatementNode(name, type);
    }
}
