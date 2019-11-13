package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class representing a variable declaration without assignment.
 *
 * @author Patrick Norton
 * @see DeclaredAssignmentNode
 */
public class DeclarationNode implements AssignStatementNode, ClassStatementNode {
    private LineInfo lineInfo;
    private TypeLikeNode type;
    private VariableNode name;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    public DeclarationNode(TypeLikeNode type, VariableNode name) {
        this(type.getLineInfo(), type, name);
    }

    /**
     * Create a new instance of DeclarationNode.
     * @param type The type of the declared variable
     * @param name The name of that variable
     */
    @Contract(pure = true)
    public DeclarationNode(LineInfo lineInfo, TypeLikeNode type, VariableNode name) {
        this.lineInfo = lineInfo;
        this.type = type;
        this.name = name;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeLikeNode getType() {
        return type;
    }

    @Override
    public NameNode[] getName() {
        return new NameNode[] {name};
    }

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.DECLARATION_VALID;
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
        TypeLikeNode type = TypeLikeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        return new DeclarationNode(type, var);
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) + type + ' ' + name;
    }
}
