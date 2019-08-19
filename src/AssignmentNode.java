public class AssignmentNode implements AssignStatementNode {
    private Boolean is_colon;
    private VariableNode[] name;
    private TestNode[] value;

    public AssignmentNode(Boolean is_colon, VariableNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.name = name;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    public SubTestNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }
}
