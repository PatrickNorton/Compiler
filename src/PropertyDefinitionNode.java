public class PropertyDefinitionNode implements DefinitionNode, ClassStatementNode {
    public VariableNode name;
    public StatementBodyNode get;
    public TypedArgumentListNode set_args;
    public StatementBodyNode set;
    public DescriptorNode[] descriptors;

    public PropertyDefinitionNode(VariableNode name, StatementBodyNode get, TypedArgumentListNode set_args, StatementBodyNode set) {
        this.name = name;
        this.get = get;
        this.set_args = set_args;
        this.set = set;
    }

    public PropertyDefinitionNode(StatementBodyNode get) {
        this.get = get;
    }

    public PropertyDefinitionNode(TypedArgumentListNode set_args, StatementBodyNode set) {
        this.set_args = set_args;
        this.set = set;
    }

    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    public VariableNode getName() {
        return name;
    }

    public StatementBodyNode getGet() {
        return get;
    }

    public StatementBodyNode getSet() {
        return set;
    }

    public TypedArgumentListNode getSet_args() {
        return set_args;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    public StatementBodyNode getBody() {
        return get;
    }
}
