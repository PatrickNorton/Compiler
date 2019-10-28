package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a node that can be used in a BodyNode.
 * <p>
 *     This node includes any node that represents an independent statement.
 *     Nodes that do not inherit from this include {@link ElifStatementNode}
 *     and {@link CaseStatementNode}. Those statements inherit directly from
 *     {@link BaseNode}. Not to be confused with {@link BaseNode}, which simply
 *     represents all nodes.
 * </p>
 *
 * @author Patrick Norton
 * @see BaseNode
 */
public interface IndependentNode extends BaseNode {
    /**
     * Parse any node at all from the list of tokens given.
     * @param tokens The list of tokens which is to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    static IndependentNode parse(@NotNull TokenList tokens) {
        tokens.passNewlines();
        switch (tokens.tokenType()) {
            case KEYWORD:
                return parseKeyword(tokens);
            case DESCRIPTOR:
                return DescribableNode.parse(tokens);
            case OPEN_BRACE:
            case NAME:
                return parseLeftVariable(tokens);
            case CLOSE_BRACE:
                throw tokens.error("Unmatched close brace");
            case COMMA:
                throw tokens.error("Unexpected comma");
            case AUG_ASSIGN:
                throw tokens.error("Unexpected operator");
            case ARROW:
                throw tokens.error("Unexpected ->");
            case DOUBLE_ARROW:
                throw tokens.error("Unexpected =>");
            case OPERATOR:
            case BOOL_OP:
            case STRING:
            case NUMBER:
                return TestNode.parse(tokens);
            case ASSIGN:
                throw tokens.error("Unexpected assignment");
            case OPERATOR_SP:
                if (tokens.tokenIs(1, "=")) {
                    return SpecialOpAssignmentNode.parse(tokens);
                } else {
                    return OperatorDefinitionNode.parse(tokens);
                }
            case OP_FUNC:
                return EscapedOperatorNode.parse(tokens);
            case COLON:
                throw tokens.error("Unexpected colon");
            case ELLIPSIS:
                return VariableNode.parseEllipsis(tokens);
            case DOT:
                throw tokens.error("Unexpected dot");
            case AT:
                return DecoratableNode.parseLeftDecorator(tokens);
            case DOLLAR:
                return AnnotatableNode.parseLeftAnnotation(tokens);
            case EPSILON:
                throw tokens.error("Unexpected EOF");
            default:
                throw new RuntimeException("Nonexistent token found");
        }
    }

    @NotNull
    static IndependentNode parseVar(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.VAR);
        if (tokens.lineContains(TokenType.ASSIGN)) {
            return AssignStatementNode.parse(tokens);
        } else if (tokens.lineContains(TokenType.AUG_ASSIGN)) {
            throw tokens.error("var cannot be used in augmented assignment");
        } else {
            return DeclarationNode.parse(tokens);
        }
    }

    /**
     * Parse any token, given that the first item in the list is a keyword.
     * @param tokens The token list to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    private static IndependentNode parseKeyword(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        Keyword kw = Keyword.find(tokens.tokenSequence());
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
    private static IndependentNode parseLeftVariable(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME, TokenType.OPEN_BRACE);
        Token after_var = tokens.getToken(tokens.sizeOfVariable());
        if (tokens.lineContains(TokenType.ASSIGN)) {
            return AssignStatementNode.parse(tokens);
        } else if (tokens.lineContains(TokenType.AUG_ASSIGN)) {
            return AugmentedAssignmentNode.parse(tokens);
        } else if (after_var.is("++", "--")) {
            return SimpleStatementNode.parseIncDec(tokens);
        } else if (tokens.lineContains(TokenType.BOOL_OP, TokenType.OPERATOR)) {
            if (after_var.is("?") && isDeclaration(tokens)) {
                return DeclarationNode.parse(tokens);
            }
            return TestNode.parse(tokens);
        } else if (after_var.is(TokenType.NAME)) {
            return DeclarationNode.parse(tokens);
        } else {
            return NameNode.parse(tokens);
        }
    }

    private static boolean isDeclaration(@NotNull TokenList tokens) {
        int varSize = tokens.sizeOfVariable();
        if (tokens.tokenIs(varSize, "?") && tokens.tokenIs(varSize + 1, TokenType.NAME)) {
            return tokens.tokenIs(tokens.sizeOfVariable(varSize + 1), TokenType.NEWLINE);
        }
        return false;
    }
}
