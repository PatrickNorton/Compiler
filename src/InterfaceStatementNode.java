import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing an interface statement.
 * @author Patrick Norton
 */
public interface InterfaceStatementNode extends BaseNode {
    void addDescriptor(DescriptorNode[] nodes);

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
    // TODO: Static blocks in interfaces
    @NotNull
    static InterfaceStatementNode parse(@NotNull TokenList tokens) {  // TODO: Clean up method and/or factor out
        if (tokens.tokenIs(TokenType.OPERATOR_SP, TokenType.DESCRIPTOR) || tokens.tokenIs("method")
              || (tokens.tokenIs("class") && tokens.getToken(1).is(TokenType.DESCRIPTOR, TokenType.KEYWORD))) {
            LinkedList<DescriptorNode> descriptors = new LinkedList<>();
            if (tokens.tokenIs("class")) {
                descriptors.add(DescriptorNode.find("class"));
                tokens.nextToken();
            }
            while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
                descriptors.add(DescriptorNode.find(tokens.getFirst().sequence));
                tokens.nextToken();
            }
            int line_size = 0;
            for (Token token : tokens) {
                if (token.is(TokenType.NEWLINE)) {
                    break;
                }
                line_size++;
            }
            if (!tokens.getToken(line_size - 1).is("{")) {
                boolean is_operator;
                VariableNode fn_name;
                String op_name;
                if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                    is_operator = true;
                    op_name = tokens.getFirst().sequence;
                    fn_name = new VariableNode();
                    tokens.nextToken();
                } else {
                    tokens.nextToken();
                    is_operator = false;
                    fn_name = VariableNode.parse(tokens);
                    op_name = "";
                }
                TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
                TypeNode[] retvals = new TypeNode[0];
                if (tokens.tokenIs(TokenType.ARROW)) {
                    retvals = TypeNode.parseRetVal(tokens);
                }
                if (is_operator) {
                    GenericOperatorNode op = new GenericOperatorNode(op_name, args, retvals);
                    if (!descriptors.isEmpty()) {
                        op.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
                    }
                    return op;
                } else {
                    GenericFunctionNode fn = new GenericFunctionNode(fn_name, args, retvals);
                    if (!descriptors.isEmpty()) {
                        fn.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
                    }
                    return fn;
                }
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
