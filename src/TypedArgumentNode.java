public class TypedArgumentNode implements SubTestNode {
    private TypeNode type;
    private VariableNode name;
    private TestNode defaultval;
    private Boolean is_vararg;
    private String vararg_type;

    public TypedArgumentNode(TypeNode type, VariableNode name, TestNode defaultval, Boolean is_vararg, String vararg_type) {
        this.type = type;
        this.name = name;
        this.defaultval = defaultval;
        this.is_vararg = is_vararg;
        this.vararg_type = vararg_type;
    }

    public TypeNode getType() {
        return type;
    }

    public VariableNode getName() {
        return name;
    }

    public TestNode getDefaultval() {
        return defaultval;
    }

    public Boolean getIs_vararg() {
        return is_vararg;
    }

    public String getVararg_type() {
        return vararg_type;
    }
}
