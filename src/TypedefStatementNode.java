public class TypedefStatementNode implements SimpleStatementNode {
    private TypeNode name;
    private TypeNode type;

    public TypedefStatementNode(TypeNode name, TypeNode type) {
        this.name = name;
        this.type = type;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }
}
