package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public interface DescribableNode extends IndependentNode {
    void addDescriptor(EnumSet<DescriptorNode> nodes);
    EnumSet<DescriptorNode> getDescriptors();

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
            ((DescribableNode) stmt).addDescriptor(descriptors);
            return (DescribableNode) stmt;
        } else {
            throw new ParserException("Descriptor not allowed in statement");
        }
    }
}
