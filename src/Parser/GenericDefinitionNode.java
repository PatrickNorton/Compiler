package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public interface GenericDefinitionNode extends InterfaceStatementNode {
    @Override
    default EnumSet<DescriptorNode> validDescriptors() {
        return DescriptorNode.DEFINITION_VALID;
    }

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
            throw new RuntimeException("Invalid sent value to isGeneric");
        }
    }
}
