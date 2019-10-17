package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing a variable declaration without assignment.
 *
 * @author Patrick Norton
 * @see DeclaredAssignmentNode
 */
public class DeclarationNode implements AssignStatementNode, ClassStatementNode {
    private LineInfo lineInfo;
    private TypeNode type;
    private VariableNode name;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    public DeclarationNode(TypeNode type, VariableNode name) {
        this(type.getLineInfo(), type, name);
    }

    /**
     * Create a new instance of DeclarationNode.
     * @param type The type of the declared variable
     * @param name The name of that variable
     */
    @Contract(pure = true)
    public DeclarationNode(LineInfo lineInfo, TypeNode type, VariableNode name) {
        this.lineInfo = lineInfo;
        this.type = type;
        this.name = name;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeNode getType() {
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
    public EnumSet<DescriptorNode> validDescriptors() {
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
        TypeNode type = TypeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
        return new DeclarationNode(type, var);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d.toString());
            sb.append(' ');
        }
        sb.append(type);
        sb.append(' ');
        sb.append(name);
        return sb.toString();
    }
}
