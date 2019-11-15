package Parser;

import org.jetbrains.annotations.NotNull;

/**
 * The interface representing any statement which can function on the left side
 * of an equals sign.
 *
 * @author Patrick Norton
 */
public interface AssignableNode extends TestNode {
    /**
     * Parse an AssignableNode from a list of tokens.
     * <p>
     *     Unlike some other interface's parse methods, which are simply
     *     delegate-and-check methods, this must actually parse itself, as
     *     delegating to {@link TestNode#parse} would attempt to parse the
     *     equals sign as well.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed AssignableNode
     */
    @NotNull
    static AssignableNode parse(@NotNull TokenList tokens) {
        assert tokens.lineContains(TokenType.ASSIGN);
        switch (tokens.tokenType()) {
            case NAME:
                return NameNode.parse(tokens);
            case NUMBER:
                if (tokens.tokenIs(1, TokenType.DOT)) {
                    NumberNode num = NumberNode.parse(tokens);
                    return DottedVariableNode.fromExpr(tokens, num);
                } else {
                    throw tokens.error("Cannot assign to numeric literal");
                }
            case STRING:
                if (tokens.tokenIs(1, TokenType.DOT)) {
                    StringLikeNode str = StringLikeNode.parse(tokens);
                    return DottedVariableNode.fromExpr(tokens, str);
                } else {
                    throw tokens.error("Cannot assign to string literal");
                }
            case OPEN_BRACE:
                TestNode t = TestNode.parseOpenBrace(tokens, false);
                if (tokens.tokenIs(TokenType.DOT)) {
                    return DottedVariableNode.fromExpr(tokens, t);
                } else if (t instanceof AssignableNode) {
                    return (AssignableNode) t;
                } else {
                    throw tokens.error("Cannot assign to node");
                }
            default:
                throw tokens.error("Unassignable word" + tokens.getFirst());
        }
    }
}
