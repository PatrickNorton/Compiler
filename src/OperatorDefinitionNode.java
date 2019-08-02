public class OperatorDefinitionNode implements DefinitionNode, ClassStatementNode {
    private String op_code;
    private TypeNode[] ret_type;
    private TypedArgumentListNode args;
    private StatementBodyNode body;
    private DescriptorNode[] descriptors;

    public OperatorDefinitionNode(String op_code, TypeNode[] ret_type, TypedArgumentListNode args, StatementBodyNode body) {
        this.op_code = op_code;
        this.ret_type = ret_type;
        if (args != null) {
            this.args = args;
        } else {
            this.args = new TypedArgumentListNode();
        }
        this.body = body;
    }

    public OperatorDefinitionNode(String op_code, StatementBodyNode body) {
        this.op_code = op_code;
        this.args = new TypedArgumentListNode();
        this.body = body;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    public String getOp_code() {
        return op_code;
    }

    public TypeNode[] getRet_type() {
        return ret_type;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public VariableNode getName() {
        return new VariableNode(op_code);
    }
}
