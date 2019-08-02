public class TypeListNode implements BaseNode {
    private TypeNode[] types;
    private Boolean[] is_vararg;

    public TypeListNode(TypeNode[] types, Boolean[] is_vararg) {
        this.types = types;
        this.is_vararg = is_vararg;
    }

    public TypeNode[] getTypes() {
        return types;
    }

    public Boolean[] getIs_vararg() {
        return is_vararg;
    }
}
