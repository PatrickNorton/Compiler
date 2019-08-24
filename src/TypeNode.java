public class TypeNode implements AtomicNode {
    private DottedVariableNode name;
    private TypeNode[] subtypes;
    private boolean is_vararg;

    public TypeNode(DottedVariableNode name) {
        this.name = name;
        this.subtypes = new TypeNode[0];
    }

    public TypeNode(DottedVariableNode name, TypeNode[] subtypes, boolean is_vararg) {
        this.name = name;
        this.subtypes = subtypes;
        this.is_vararg = is_vararg;
    }

    public DottedVariableNode getName() {
        return name;
    }

    public TypeNode[] getSubtypes() {
        return subtypes;
    }

    public boolean getIs_vararg() {
        return is_vararg;
    }
}
