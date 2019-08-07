public class GenericFunctionNode implements InterfaceStatementNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private DescriptorNode[] descriptors;

    public GenericFunctionNode(VariableNode name, TypedArgumentListNode args, TypeNode... retvals) {
        this.name = name;
        this.args = args;
        this.retvals = retvals;
    }

    public VariableNode getName() {
        return name;
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
