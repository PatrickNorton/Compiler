public class TypedVariableNode implements SubTestNode {
    TypeNode type;
    VariableNode var;

    public TypedVariableNode(TypeNode type, VariableNode var) {
        this.type = type;
        this.var = var;
    }
}
