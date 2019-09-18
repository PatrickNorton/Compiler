import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a typed argument.
 * @author Patrick Norton
 * @see TypedArgumentListNode
 */
public class TypedArgumentNode implements BaseNode {
    private TypeNode type;
    private VariableNode name;
    private TestNode defaultval;
    private Boolean is_vararg;
    private String vararg_type;

    /**
     * Construct a new instance of TypedArgumentListNode.
     * @param type The type of the argument
     * @param name The name of the argument
     * @param defaultval The default value of the argument
     * @param is_vararg If the argument has a vararg
     * @param vararg_type The type of the vararg, if it exists
     */
    @Contract(pure = true)
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

    /**
     * Parse a TypedArgumentNode from a list of tokens.
     * <p>
     *     The syntax for a typed argument is: <code>["*" | "**"] {@link
     *     TypeNode} {@link VariableNode} ["=" {@link TestNode}]</code>.
     * </p>
     * @param tokens The list of tokens to destructively parse
     * @return The newly parsed TypedArgumentNode
     */
    @NotNull
    @Contract("_ -> new")
    static TypedArgumentNode parse(@NotNull TokenList tokens) {
        boolean is_vararg = tokens.tokenIs("*", "**");
        String vararg_type;
        if (tokens.tokenIs("*", "**")) {
            vararg_type = tokens.getFirst().sequence;
            tokens.nextToken();
        } else {
            vararg_type = "";
        }
        TypeNode type = TypeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        TestNode default_value = TestNode.empty();
        if (tokens.tokenIs("=")) {
            tokens.nextToken();
            default_value = TestNode.parse(tokens, true);
        }
        return new TypedArgumentNode(type, var, default_value, is_vararg, vararg_type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (is_vararg) {
            sb.append(vararg_type);
        }
        sb.append(type);
        sb.append(" ");
        sb.append(name);
        if (!defaultval.isEmpty()) {
            sb.append("=");
            sb.append(defaultval);
        }
        return sb.toString();
    }
}
