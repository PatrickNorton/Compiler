package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public interface DescribableNode extends IndependentNode {

    /**
     * Add a descriptor to the node
     * @param nodes The descriptors to add
     */
    void addDescriptor(EnumSet<DescriptorNode> nodes);

    /**
     * Get the descriptors of the node
     * @return The descriptors
     */
    EnumSet<DescriptorNode> getDescriptors();

    /**
     * Get the valid descriptors for a node
     * @return The descriptors of a node
     */
    default Set<DescriptorNode> validDescriptors() {
        return EnumSet.allOf(DescriptorNode.class);
    }

    /**
     * Parse a describable node from a list of tokens.
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed DescribableNode
     */
    @NotNull
    static DescribableNode parse(@NotNull TokenList tokens) {
        EnumSet<DescriptorNode> descriptors;
        if (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors = DescriptorNode.parseList(tokens);
        } else {
            descriptors = DescriptorNode.emptySet();
        }
        IndependentNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof DescribableNode) {
            if (!((DescribableNode) stmt).validDescriptors().containsAll(descriptors)) {
                throw tokens.error("Invalid descriptor " + descriptors + " for this set");
            }
            ((DescribableNode) stmt).addDescriptor(descriptors);
            return (DescribableNode) stmt;
        } else {
            throw tokens.error("Descriptor not allowed in statement");
        }
    }
}
