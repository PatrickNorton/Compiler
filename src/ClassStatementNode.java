import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The interface for statements which are valid in a class body.
 * <p>
 *     This inherits the method {@code addDescriptor} from
 *     InterfaceStatementNode, this allows all statements in a class body to
 *     add descriptors to themselves
 * </p>
 *
 * @author Patrick Norton
 * @see InterfaceStatementNode
 */
public interface ClassStatementNode extends InterfaceStatementNode {
    /**
     * Parse a ClassStatementNode from a list of tokens.
     * <p>
     *     ClassStatementNodes are simply any {@link BaseNode} which inherits
     *     from this node, and that is how they are parsed, with a call to
     *     {@link BaseNode#parse}, and a typecast filter. This node
     *     is defined as the union of all its subclasses, check an inheritance
     *     tree or grep for those.
     * </p>
     * @param tokens Tokens to be parsed; parse does operate destructively on
     *               them
     * @return The newly parsed ClassStatementNode
     */
    @NotNull
    static ClassStatementNode parse(@NotNull TokenList tokens) {
        if (tokens.tokenIs("static") && tokens.getToken(1).is("{")) {
            tokens.nextToken();
            return new StaticBlockNode(StatementBodyNode.parse(tokens));  // TODO? StaticBlockNode.parse
        }
        BaseNode stmt = BaseNode.parse(tokens);
        if (stmt instanceof ClassStatementNode) {
            return (ClassStatementNode) stmt;
        }
        throw new ParserException(tokens.getFirst()+" is not a valid class statement");
    }

    /**
     * Parse a list of tokens for anything starting with a descriptor.
     * <p>
     *     This delegates to three helper methods based on the next token in
     *     the list. The first token in the list must be a descriptor, and not
     *     anything else. The token after all descriptors must be either a
     *     keyword, special operator, or a name. All of these must then form
     *     a valid ClassStatementNode.
     * </p>
     * @param tokens The tokens to be parsed destructively
     * @return The newly parsed ClassStatementNode
     */
    @NotNull
    static ClassStatementNode parseDescriptor(@NotNull TokenList tokens) {
        ArrayList<DescriptorNode> descriptors = new ArrayList<>();
        if (tokens.tokenIs("class")) {
            descriptors.add(DescriptorNode.find("class"));
            tokens.nextToken();
        }
        while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors.add(DescriptorNode.find(tokens.getFirst().sequence));
            tokens.nextToken();
        }
        assert descriptors.size() > 0;
        switch (tokens.getFirst().token) {
            case KEYWORD:
                return parseDescriptorKeyword(tokens, descriptors);
            case OPERATOR_SP:
                return parseDescriptorOpSp(tokens, descriptors);
            case NAME:
                return parseDescriptorVar(tokens, descriptors);
            default:
                throw new ParserException("Invalid descriptor placement");
        }
    }

    /**
     * Parse a statement beginning with a keyword, which has been preceded by
     * one or more descriptors.
     * <p>
     *     There are three keyword-beginning items which may be preceded by a
     *     descriptor. These are {@link ClassDefinitionNode}, {@link
     *     MethodDefinitionNode}, and {@link InterfaceDefinitionNode}. All of
     *     these have descriptors added to them at the end of the
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param descriptors The list of descriptors which goes in front of them
     * @return The descriptor-added node
     */
    @NotNull
    @Contract("_, _ -> new")
    private static ClassStatementNode parseDescriptorKeyword(@NotNull TokenList tokens, @NotNull ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        ClassStatementNode node;
        switch (tokens.getFirst().sequence) {
            case "class":
                node = ClassDefinitionNode.parse(tokens);
                break;
            case "method":
                node = MethodDefinitionNode.parse(tokens);
                break;
            case "interface":
                node = InterfaceDefinitionNode.parse(tokens);
                break;
            case "func":
            case "if":
            case "for":
            case "elif":
            case "else":
            case "do":
            case "dotimes":
            case "while":
            case "in":
            case "from":
            case "import":
            case "export":
            case "typeget":
            case "break":
            case "continue":
            case "return":
                throw new ParserException("Expected descriptor-usable keyword, got "+tokens.getFirst());
            default:
                throw new RuntimeException("Keyword mismatch");
        }
        node.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return node;
    }

    /**
     * Parses a special operator preceded by a descriptor.
     * <p>
     *     This is simply a delegator method to {@link
     *     OperatorDefinitionNode#parse(TokenList)}, with the caveat that it
     *     adds on the parsed descriptors at the end.
     * </p>
     * @param tokens List of tokens to be parsed destructively
     * @param descriptors List of descriptors to be added on
     * @return The descriptor-added node
     */
    @NotNull
    private static ClassStatementNode parseDescriptorOpSp(@NotNull TokenList tokens, @NotNull ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        OperatorDefinitionNode op_sp = OperatorDefinitionNode.parse(tokens);
        op_sp.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return op_sp;
    }

    /**
     * Parses a name preceded by a descriptor.
     * <p>
     *     This is a delegator to one of two functions, either {@link
     *     DeclaredAssignmentNode#parse} or {@link
     *     DeclarationNode#parse}, depending on whether or not there
     *     is an assignment happening.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param descriptors The descriptors to be added onto the end
     * @return The statement with added descriptors
     */
    @NotNull
    private static ClassStatementNode parseDescriptorVar(@NotNull TokenList tokens, @NotNull ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.NAME);
        ClassStatementNode stmt;
        if (tokens.lineContains(TokenType.ASSIGN)) {
            stmt = DeclaredAssignmentNode.parse(tokens);
        } else {
            stmt = DeclarationNode.parse(tokens);
        }
        stmt.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return stmt;
    }
}
