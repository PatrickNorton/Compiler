public class TypeNode implements AtomicNode {
    private String name;
    private TypeNode[] subtypes;
    private Boolean[] is_vararg;

    public TypeNode(String name) {
        this.name = name;
        this.subtypes = new TypeNode[0];
    }

    public TypeNode(String name, TypeNode[] subtypes, Boolean[] is_vararg) {
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

    public Boolean[] getIs_vararg() {
        return is_vararg;
    }
}
