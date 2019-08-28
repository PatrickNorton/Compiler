public class DecrementNode implements SimpleStatementNode {
    private NameNode variable;

    public DecrementNode(NameNode variable) {
        this.variable = variable;
    }

    public NameNode getVariable() {
        return variable;
    }

    static DecrementNode parse(TokenList tokens) {
        NameNode var = NameNode.parse(tokens);
        if (!tokens.tokenIs("--")) {
            throw new RuntimeException("Expected --, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DecrementNode(var);
    }
}
