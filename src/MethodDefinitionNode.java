public class MethodDefinitionNode implements DefinitionNode, ClassStatementNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retval;
    private StatementBodyNode body;
    private DescriptorNode[] descriptors;

    public MethodDefinitionNode(VariableNode name, TypedArgumentListNode args, TypeNode[] retval, StatementBodyNode body) {
        this.name = name;
        this.args = args;
        this.retval = retval;
        this.body = body;
    }

    @Override
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

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    static MethodDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs("method");
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        tokens.Newline();
        return new MethodDefinitionNode(name, args, retval, body);
    }
}
