package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The interface representing a definition of a generic function.
 *
 * @author Patrick Norton
 */
public interface GenericDefinitionNode extends InterfaceStatementNode {
    @Override
    default EnumSet<DescriptorNode> validDescriptors() {
        return DescriptorNode.DEFINITION_VALID;
    }

    /**
     * Find whether or not the upcoming definition is generic or not.
     * @param tokens The list of tokens to check
     * @return Whether or not the function is generic
     */
    static boolean isGeneric(@NotNull TokenList tokens) {
        if (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            int descriptorSize = DescriptorNode.count(tokens);
            if (tokens.tokenIs(descriptorSize, TokenType.OPERATOR_SP)) {
                return GenericOperatorNode.isGeneric(tokens, descriptorSize);
            } else if (tokens.tokenIs(descriptorSize, Keyword.METHOD)) {
                return GenericFunctionNode.isGeneric(tokens, descriptorSize);
            } else {
                return false;
            }
        } else if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            return GenericOperatorNode.isGeneric(tokens);
        } else if (tokens.tokenIs(Keyword.METHOD)) {
            return GenericFunctionNode.isGeneric(tokens);
        } else {
            throw tokens.internalError("Invalid sent value to isGeneric");
        }
    }
}
