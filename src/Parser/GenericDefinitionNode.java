package Parser;

import org.jetbrains.annotations.NotNull;

public interface GenericDefinitionNode extends InterfaceStatementNode {
    static boolean isGeneric(@NotNull TokenList tokens) {
        if (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            int descriptorSize = DescriptorNode.count(tokens);
            if (tokens.tokenIs(descriptorSize, TokenType.OPERATOR_SP)) {
                return GenericOperatorNode.isGeneric(tokens, descriptorSize);
            } else if (tokens.tokenIs(descriptorSize, "method")) {
                return GenericFunctionNode.isGeneric(tokens, descriptorSize);
            } else {
                return false;
            }
        } else if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            return GenericOperatorNode.isGeneric(tokens);
        } else if (tokens.tokenIs("method")) {
            return GenericFunctionNode.isGeneric(tokens);
        } else {
            throw new RuntimeException("Invalid sent value to isGeneric");
        }
    }
}
