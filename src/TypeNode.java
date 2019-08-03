public class TypeNode implements AtomicNode {
    private String name;
    private TypeNode[] subtypes;
    private boolean is_vararg;

    public TypeNode(String name) {
        this.name = name;
        this.subtypes = new TypeNode[0];
    }

    public TypeNode(String name, TypeNode[] subtypes, boolean is_vararg) {
        this.name = name;
        this.subtypes = subtypes;
        this.is_vararg = is_vararg;
    }

    public String getName() {
        return name;
    }

    public TypeNode[] getSubtypes() {
        return subtypes;
    }

    public boolean getIs_vararg() {
        return is_vararg;
    }
}
