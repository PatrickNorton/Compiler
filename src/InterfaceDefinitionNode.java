public class InterfaceDefinitionNode implements ComplexStatementNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private InterfaceBodyNode body;
    private DescriptorNode[] descriptors;

    public InterfaceDefinitionNode(TypeNode name, TypeNode[] superclasses, InterfaceBodyNode body) {
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
        this.descriptors = null;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode[] getSuperclasses() {
        return superclasses;
    }

    @Override
    public InterfaceBodyNode getBody() {
        return body;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }
}
