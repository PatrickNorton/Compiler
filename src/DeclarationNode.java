public class DeclarationNode implements AssignStatementNode, ClassStatementNode {
    private TypeNode type;
    private VariableNode name;
    private DescriptorNode[] descriptors;

    public DeclarationNode(TypeNode type, VariableNode name) {
        this.type = type;
        this.name = name;
    }

    public TypeNode getType() {
        return type;
    }

    public NameNode[] getName() {
        return new NameNode[] {name};
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }
}
