import java.util.LinkedList;

public class TypedVariableNode implements SubTestNode {
    private TypeNode type;
    private VariableNode var;

    public TypedVariableNode(TypeNode type, VariableNode var) {
        this.type = type;
        this.var = var;
    }

    public TypeNode getType() {
        return type;
    }

    public VariableNode getVar() {
        return var;
    }

    static TypedVariableNode[] parseList(TokenList tokens) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (true) {
            vars.add(TypedVariableNode.parse(tokens));
            if (tokens.tokenIs("in")) {
                break;
            }
            if (!tokens.tokenIs(",")) {
                throw new ParserException("Unexpected "+tokens.getFirst());
            }
            tokens.nextToken();
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    static TypedVariableNode parse(TokenList tokens) {
        TypeNode type = TypeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        return new TypedVariableNode(type, var);
    }

    static TypedVariableNode[] parseForVars(TokenList tokens) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (!tokens.tokenIs("in")) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                throw new ParserException("Expected variable, got " + tokens.getFirst());
            }
            vars.add(TypedVariableNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
            }
        }
        return vars.toArray(new TypedVariableNode[0]);
    }
}
