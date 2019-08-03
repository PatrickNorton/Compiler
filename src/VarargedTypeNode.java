public class VarargedTypeNode implements SubTestNode {
    boolean vararg;
    TypeNode type;

    public VarargedTypeNode(boolean is_vararg, TypeNode type) {
        this.vararg = is_vararg;
        this.type = type;
    }

    public boolean isVararg() {
        return vararg;
    }

    public TypeNode getType() {
        return type;
    }
}
