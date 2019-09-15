import org.jetbrains.annotations.NotNull;

/**
 * The class representing an interface statement.
 * @author Patrick Norton
 */
public interface InterfaceStatementNode extends BaseNode {
    void addDescriptor(DescriptorNode[] nodes);
    DescriptorNode[] getDescriptors();

    /**
     * Parse an interface statement from a list of tokens.
     * <p>
     *     An interface statement is simply any statement which inherits from
     *     this, and that is how it is parsed, with the special case that
     *     generic functions and operators are parsed specially here.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed interface statement
     */
    @NotNull
    static InterfaceStatementNode parse(@NotNull TokenList tokens) {  // TODO: Clean up method and/or factor out
        if (tokens.tokenIs("static") && tokens.tokenIs(1, "{")) {
            return StaticBlockNode.parse(tokens);
        }
        if (tokens.tokenIs(TokenType.OPERATOR_SP) && GenericOperatorNode.isGeneric(tokens)) {
            return GenericOperatorNode.parse(tokens);
        }
        if (tokens.tokenIs("method") && GenericFunctionNode.isGeneric(tokens)) {
            return GenericFunctionNode.parse(tokens);
        }
        if (tokens.tokenIs(TokenType.DESCRIPTOR) && GenericDefinitionNode.isGeneric(tokens)) {
            DescriptorNode[] descriptors = DescriptorNode.parseList(tokens);
            GenericDefinitionNode op;
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                op = GenericOperatorNode.parse(tokens);
                op.addDescriptor(descriptors);
                return op;
            } else if (tokens.tokenIs("method")) {
                op = GenericFunctionNode.parse(tokens);
                op.addDescriptor(descriptors);
                return op;
            }
        }
        BaseNode stmt = BaseNode.parse(tokens);
        if (stmt instanceof InterfaceStatementNode) {
            return (InterfaceStatementNode) stmt;
        } else {
            throw new ParserException("Illegal statement");
        }
    }
}
