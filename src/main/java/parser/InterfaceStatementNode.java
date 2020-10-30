package main.java.parser;

import java.util.EnumSet;

/**
 * The class representing an interface statement.
 *
 * @author Patrick Norton
 */
public interface InterfaceStatementNode extends IndependentNode, DescribableNode {

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

    static InterfaceStatementNode parse(TokenList tokens) {  // TODO: Clean up method and/or factor out
        if (tokens.tokenIs("static") && tokens.tokenIs(1, "{")) {
            return StaticBlockNode.parse(tokens);
        }
        EnumSet<DescriptorNode> descriptors = DescriptorNode.parseList(tokens);
        InterfaceStatementNode op;
        if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            if (tokens.tokenIs(1, TokenType.ASSIGN)) {
                op = SpecialOpAssignmentNode.parse(tokens);
            } else {
                op = GenericOperatorNode.parse(tokens);
                if (tokens.tokenIs("{")) {
                    op = OperatorDefinitionNode.fromGeneric(tokens, (GenericOperatorNode) op);
                }
            }
        } else if (tokens.tokenIs(Keyword.METHOD)) {
            op = GenericFunctionNode.parse(tokens);
            if (tokens.tokenIs("{")) {
                op = MethodDefinitionNode.fromGeneric(tokens, (GenericFunctionNode) op);
            }
        } else {
            BaseNode stmt = IndependentNode.parse(tokens);
            if (stmt instanceof InterfaceStatementNode) {
                op = (InterfaceStatementNode) stmt;
            } else {
                throw ParserException.of("Illegal statement in interface definition", stmt);
            }
        }
        op.addDescriptor(descriptors);
        return op;
    }
}
