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
}
