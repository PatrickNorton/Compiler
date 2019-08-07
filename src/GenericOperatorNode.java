public class GenericOperatorNode implements InterfaceStatementNode {
    private String op_code;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private DescriptorNode[] descriptors;

    public GenericOperatorNode(String op_code, TypedArgumentListNode args, TypeNode... retvals) {
        this.op_code = op_code;
        this.args = args;
        this.retvals = retvals;
    }

    public String getOp_code() {
        return op_code;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetvals() {
        return retvals;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }
}
