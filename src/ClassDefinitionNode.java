public class ClassDefinitionNode implements DefinitionNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private ClassBodyNode body;
    private DescriptorNode[] descriptors;

    public ClassDefinitionNode(TypeNode name, TypeNode[] superclasses, ClassBodyNode body) {
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
    }

    public ClassDefinitionNode(TypeNode name, ClassBodyNode body) {
        this.name = name;
        this.superclasses = new TypeNode[0];
        this.body = body;
    }

    public void addDescriptor(DescriptorNode[] descriptors) {
        this.descriptors = descriptors;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode[] getSuperclasses() {
        return superclasses;
    }

    public ClassBodyNode getBody() {
        return body;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }
}
