package Parser;

import org.jetbrains.annotations.NotNull;

/**
 * The class representing an enum keyword.
 *
 * @author Patrick Norton
 * @see EnumDefinitionNode
 */
public interface EnumKeywordNode extends NameNode {
    /**
     * Get the variable name for an enum keyword.
     * @return The variable name
     */
    VariableNode getVariable();

    /**
     * Parse an enum keyword from a list of tokens.
     * <p>
     *     An enum keyword can only be one of two things: a {@link NameNode} or
     *     a {@link FunctionCallNode} with a {@link NameNode} as callee.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed EnumKeywordNode
     */
    @NotNull
    static EnumKeywordNode parse(@NotNull TokenList tokens) {
        if (!tokens.tokenIs(TokenType.NAME)) {
            throw new ParserException("Enum keyword must start with a variable name");
        }
        NameNode t = NameNode.parse(tokens);
        if (t instanceof EnumKeywordNode) {
            return (EnumKeywordNode) t;
        } else {
            throw new ParserException("Unexpected keyword");
        }
    }
}
