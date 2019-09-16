import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The base for all nodes.
 * <p>
 *     This class has two purposes: to function as a base for all other nodes,
 *     and to hold the delegator methods which are to call other parsing
 *     methods.
 * </p>
 * @author Patrick Norton
 */
public interface BaseNode {
    /**
     * Parse any node at all from the list of tokens given.
     * @param tokens The list of tokens which is to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    static BaseNode parse(@NotNull TokenList tokens) {
        tokens.passNewlines();
        switch (tokens.getFirst().token) {
            case KEYWORD:
                return parseKeyword(tokens);
            case DESCRIPTOR:
                return ClassStatementNode.parseDescriptor(tokens);
            case OPEN_BRACE:
                if (tokens.lineContains(TokenType.ASSIGN)) {
                    return AssignStatementNode.parse(tokens);
                } else {
                    return TestNode.parse(tokens);
                }
            case CLOSE_BRACE:
                throw new ParserException("Unmatched close brace");
            case NAME:
                return parseLeftVariable(tokens);
            case COMMA:
                throw new ParserException("Unexpected comma");
            case AUG_ASSIGN:
                throw new ParserException("Unexpected operator");
            case ARROW:
                throw new ParserException("Unexpected ->");
            case OPERATOR:
                return OperatorNode.parse(tokens, false);
            case ASSIGN:
                throw new ParserException("Unexpected assignment");
            case STRING:
                return StringNode.parse(tokens);
            case BOOL_OP:
                return OperatorNode.parseBoolOp(tokens, false);
            case NUMBER:
                return NumberNode.parse(tokens);
            case OPERATOR_SP:
                if (tokens.tokenIs(1, "=")) {
                    return SpecialOpAssignmentNode.parse(tokens);
                } else {
                    return OperatorDefinitionNode.parse(tokens);
                }
            case OP_FUNC:
                return TestNode.parseOpFunc(tokens);
            case COLON:
                throw new ParserException("Unexpected colon");
            case ELLIPSIS:
                return VariableNode.parseEllipsis(tokens);
            case DOT:
                throw new ParserException("Unexpected dot");
            case AT:
                return DecoratableNode.parseLeftDecorator(tokens);
            case EPSILON:
                throw new ParserException("Unexpected EOF");
            default:
                throw new RuntimeException("Nonexistent token found");
        }
    }

    /**
     * Parse any token, given that the first item in the list is a keyword.
     * @param tokens The token list to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    private static BaseNode parseKeyword(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        Keyword kw = Keyword.find(tokens.getFirst().sequence);
        return kw.parseLeft(tokens);
    }

    /**
     * Parse a node, given that it starts with a variable token.
     * <p>
     *     <ul>
     *         <lh>Things starting with a variable token:</lh>
     *      <li>Function call
     *      <li>Lone variable, just sitting there
     *      <li>Declaration
     *      <li>Declared assignment
     *      <li>Assignment
     *      <li>Lone expression
     *     </ul>
     * </p>
     * @param tokens List of tokens to be parsed
     * @return The parsed token
     */
    private static BaseNode parseLeftVariable(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME);
        Token after_var = tokens.getToken(tokens.sizeOfVariable());
        if (tokens.lineContains(TokenType.ASSIGN)) {
            return AssignStatementNode.parse(tokens);
        } else if (tokens.lineContains(TokenType.AUG_ASSIGN)) {
            DottedVariableNode var = DottedVariableNode.parseName(tokens);
            if (!tokens.tokenIs(TokenType.AUG_ASSIGN)) {
                throw new ParserException("Expected augmented assignment, got " + tokens.getFirst());
            }
            OperatorTypeNode op = OperatorTypeNode.parse(tokens);
            TestNode assignment = TestNode.parse(tokens);
            return new AugmentedAssignmentNode(op, var, assignment);
        } else if (after_var.is("++", "--")) {
            return SimpleStatementNode.parseIncDec(tokens);
        } else if (tokens.lineContains(TokenType.BOOL_OP, TokenType.OPERATOR)) {
            return TestNode.parse(tokens);
        } else if (after_var.is(TokenType.NAME)) {
            return DeclarationNode.parse(tokens);
        } else {
            DottedVariableNode var = DottedVariableNode.parseName(tokens);
            return var;
        }
    }
}
