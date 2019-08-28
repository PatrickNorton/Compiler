public class FunctionDefinitionNode implements DefinitionNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retval;
    private StatementBodyNode body;

    public FunctionDefinitionNode(VariableNode name, TypedArgumentListNode args, TypeNode[] retval, StatementBodyNode body) {
        this.name = name;
        this.args = args;
        this.retval = retval;
        this.body = body;
    }

    public VariableNode getName() {
        return name;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetval() {
        return retval;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    static FunctionDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs("func");
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        tokens.Newline();
        return new FunctionDefinitionNode(name, args, retval, body);
    }
}
