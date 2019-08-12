public class CastStatementNode implements SimpleStatementNode {
    private VariableNode casted;
    private TypeNode type;
    private VariableNode new_name;

    public CastStatementNode(VariableNode casted, TypeNode type, VariableNode new_name) {
        this.casted = casted;
        this.type = type;
        this.new_name = new_name;
    }

    public VariableNode getCasted() {
        return casted;
    }

    public TypeNode getType() {
        return type;
    }

    public VariableNode getNew_name() {
        return new_name;
    }
}
