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

    static DeclarationNode parse(TokenList tokens) {  // REFACTORED: DeclarationNode.parse
        TypeNode type = TypeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        return new DeclarationNode(type, var);
    }
}
