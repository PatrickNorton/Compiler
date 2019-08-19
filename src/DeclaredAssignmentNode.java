public class DeclaredAssignmentNode implements AssignStatementNode, ClassStatementNode {
    private Boolean is_colon;
    private TypeNode[] type;
    private NameNode[] name;
    private TestNode[] value;
    private DescriptorNode[] descriptors;

    public DeclaredAssignmentNode(Boolean is_colon, TypeNode[] type, NameNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    public TypeNode[] getType() {
        return type;
    }

    public NameNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }
}
