package Parser;

import org.jetbrains.annotations.Contract;
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
            DescribableNode statement = (DescribableNode) stmt;
            if (!statement.validDescriptors().containsAll(descriptors)) {
                throw ParserException.of(errorMessage(statement, descriptors), statement);
            }
            statement.addDescriptor(descriptors);
            return statement;
        } else {
            throw tokens.error("Descriptor not allowed in statement");
        }
    }

    @NotNull
    @Contract(pure = true)
    private static String errorMessage(@NotNull DescribableNode stmt, @NotNull EnumSet<DescriptorNode> descriptors) {
        Set<DescriptorNode> disjoint = EnumSet.copyOf(stmt.getDescriptors());
        disjoint.removeAll(descriptors);
        return "Invalid descriptor(s): " + TestNode.toString(disjoint) + " not allowed in statement";
    }
}
