import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a declaration and assignment rolled into one.
 *
 * @author Patrick Norton
 * @see DeclarationNode
 * @see AssignmentNode
 */
public class DeclaredAssignmentNode implements AssignStatementNode, ClassStatementNode {
    private Boolean is_colon;
    private TypeNode[] type;
    private NameNode[] name;
    private TestNode[] value;
    private DescriptorNode[] descriptors;

    /**
     * Create new instance of DeclaredAssignmentNode.
     * @param is_colon Whether the assignment is dynamic (true) or static (false)
     * @param type The types of the assigned variables
     * @param name The names of those variables
     * @param value The values being assigned
     */
    // TODO: Refactor such that types and names are in a TypedVariableNode instead
    @Contract(pure = true)
    public DeclaredAssignmentNode(Boolean is_colon, TypeNode[] type, NameNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    public TypeNode[] getType() {
        return type;
    }

    @Override
    public NameNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    /**
     * Parse a DeclaredAssignmentNode from a list of tokens.
     * <p>
     *     The syntax for a DeclaredAssignmentNode is: <code>{@link TypeNode}
     *     {@link VariableNode} *["," [{@link TypeNode}] {@link VariableNode}]
     *     [","] ("=" | ":=") {@link TestNode} *["," {@link TestNode}] [","]
     *     </code>. This code is simply a delegate-and-check to
     *     {@link AssignStatementNode#parse}, simply because it is easier to
     *     do it that way rather than the other way around.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed DeclaredAssignmentNode
     */
    @NotNull
    @Contract("_ -> new")
    static DeclaredAssignmentNode parse(TokenList tokens) {
        AssignStatementNode assign = AssignStatementNode.parse(tokens);
        if (assign instanceof DeclaredAssignmentNode) {
            return (DeclaredAssignmentNode) assign;
        }
        throw new ParserException("Expected declared assignment, got normal assignment");
    }
}
