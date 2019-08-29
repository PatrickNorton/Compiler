public class IncrementNode implements SimpleStatementNode {
    private NameNode variable;

    public IncrementNode(NameNode variable) {
        this.variable = variable;
    }

    public NameNode getVariable() {
        return variable;
    }

    static IncrementNode parse(TokenList tokens) {
        NameNode var = DottedVariableNode.parseName(tokens);
        if (!tokens.tokenIs("++")) {
            throw new RuntimeException("Expected ++, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new IncrementNode(var);
    }
}
