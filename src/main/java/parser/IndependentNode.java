package main.java.parser;

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
            case AUG_ASSIGN:
                throw tokens.error("Unexpected operator");
            case OPERATOR:
            case STRING:
            case NUMBER:
            case OP_FUNC:
                return TestNode.parse(tokens);
            case ASSIGN:
                throw tokens.error("Unexpected assignment");
            case OPERATOR_SP:
                if (tokens.tokenIs(1, TokenType.ASSIGN)) {
                    return SpecialOpAssignmentNode.parse(tokens);
                } else {
                    return OperatorDefinitionNode.parse(tokens);
                }
            case ELLIPSIS:
                return VariableNode.parseEllipsis(tokens);
            case AT:
                return DecoratableNode.parseLeftDecorator(tokens);
            case DOLLAR:
                return AnnotatableNode.parseLeftAnnotation(tokens);
            case EPSILON:
                throw tokens.error("Unexpected EOF");
            case ARROW:
            case DOUBLE_ARROW:
            case INCREMENT:
            case COLON:
            case DOT:
            case COMMA:
                throw tokens.defaultError();
            default:
                throw tokens.internalError("Nonexistent token found");
        }
    }

    @NotNull
    static DescribableNode parseVar(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.VAR);
        if (tokens.lineContains(TokenType.ASSIGN)) {
            return (DescribableNode) AssignStatementNode.parse(tokens);
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
        Keyword kw = Keyword.find(tokens.getFirst());
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
        Token afterVar = tokens.getToken(tokens.sizeOfVariable());
        if (isAssignment(tokens) || isNormalAssignment(tokens)) {
            return AssignStatementNode.parse(tokens);
        } else if (tokens.tokenIs(tokens.sizeOfVariable(), TokenType.AUG_ASSIGN)) {
            return AugmentedAssignmentNode.parse(tokens);
        } else if (afterVar.is(TokenType.INCREMENT)) {
            return SimpleStatementNode.parseIncDec(tokens);
        } else if (isDeclaration(tokens)) {
            return DeclarationNode.parse(tokens);
        } else {
            return TestNode.parse(tokens);
        }
    }

    /**
     * Figure out if the upcoming sequence of tokens is assignment or not.
     * <p>
     *     This exists over the more simple {@link TokenList#lineContains}
     *     methods because it tends to parse much less of the line, leading to
     *     more efficient parsing overall.
     * </p>
     *
     * @param tokens The list of tokens to check
     * @return If there is a legal assignment ahead
     */
    private static boolean isAssignment(TokenList tokens) {
        int from = 0;
        while (true) {
            int varSize = TypeLikeNode.sizeOfType(tokens, from);
            if (varSize == 0) {
                return tokens.tokenIs(tokens.sizeOfVariable(from), TokenType.ASSIGN);
            } else if (tokens.tokenIs(varSize, TokenType.ASSIGN)) {
                return true;
            } else if (!tokens.tokenIs(varSize, TokenType.NAME)) {
                return false;
            }
            int newVarSize = tokens.sizeOfVariable(varSize);
            if (tokens.tokenIs(newVarSize, TokenType.COMMA)) {
                from = newVarSize + 1;
            } else {
                return tokens.tokenIs(newVarSize, TokenType.ASSIGN);
            }
        }
    }

    private static boolean isNormalAssignment(TokenList tokens) {
        int from = 0;
        while (TestNode.nextIsTest(tokens)) {
            int varSize = tokens.sizeOfVariable(from);
            if (tokens.tokenIs(varSize, TokenType.ASSIGN)) {
                return true;
            } else if (tokens.tokenIs(varSize, TokenType.COMMA)) {
                from = varSize + 1;
            } else {
                return false;
            }
        }
        return false;
    }

    private static boolean isDeclaration(@NotNull TokenList tokens) {
        int varSize = TypeLikeNode.sizeOfType(tokens, 0);
        return varSize > 0
                && tokens.tokenIs(varSize, TokenType.NAME)
                && tokens.tokenIs(varSize + 1, TokenType.NEWLINE);
    }
}
