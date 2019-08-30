import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a variable declaration without assignment.
 *
 * @author Patrick Norton
 * @see DeclaredAssignmentNode
 */
public class DeclarationNode implements AssignStatementNode, ClassStatementNode {
    private TypeNode type;
    private VariableNode name;
    private DescriptorNode[] descriptors;

    /**
     * Create a new instance of DeclarationNode.
     * @param type The type of the declared variable
     * @param name The name of that variable
     */
    @Contract(pure = true)
    public DeclarationNode(TypeNode type, VariableNode name) {
        this.type = type;
        this.name = name;
    }

    public TypeNode getType() {
        return type;
    }

    @Override
    public NameNode[] getName() {
        return new NameNode[] {name};
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    /**
     * Parse a DeclarationNode from a list of tokens.
     * <p>
     *     The syntax for a DeclarationNode is: <code>{@link TypeNode}
     *     {@link VariableNode}</code>. It is important that there be a check
     *     done previously to determine that this is not simply part of a
     *     larger node, such as a {@link DeclaredAssignmentNode}, or other
     *     such thing.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed DeclarationNode
     */
    @NotNull
    static DeclarationNode parse(TokenList tokens) {
        TypeNode type = TypeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        return new DeclarationNode(type, var);
    }
}
