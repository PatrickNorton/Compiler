public class AssignmentNode implements AssignStatementNode {
    private Boolean is_colon;
    private NameNode[] name;
    private TestNode[] value;

    public AssignmentNode(Boolean is_colon, NameNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.name = name;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    public NameNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }
}
