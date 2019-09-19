package Parser;

import org.jetbrains.annotations.NotNull;

public interface AssignableNode extends TestNode {
    @NotNull
    static AssignableNode parse(@NotNull TokenList tokens) {
        assert tokens.lineContains(TokenType.ASSIGN);
        switch (tokens.getFirst().token) {
            case NAME:
                return NameNode.parse(tokens);
            case NUMBER:
                if (tokens.tokenIs(1, TokenType.DOT)) {
                    NumberNode num = NumberNode.parse(tokens);
                    return DottedVariableNode.fromExpr(tokens, num);
                } else {
                    throw new ParserException("Cannot assign to numeric literal");
                }
            case STRING:
                if (tokens.tokenIs(1, TokenType.DOT)) {
                    StringLikeNode str = StringNode.parse(tokens);
                    return DottedVariableNode.fromExpr(tokens, str);
                } else {
                    throw new ParserException("Cannot assign to string literal");
                }
            case OPEN_BRACE:
                TestNode t = TestNode.parseOpenBrace(tokens);
                if (tokens.tokenIs(TokenType.DOT)) {
                    return DottedVariableNode.fromExpr(tokens, t);
                } else if (t instanceof AssignableNode) {
                    return (AssignableNode) t;
                } else {
                    throw new ParserException("Cannot assign to node");
                }
            default:
                throw new ParserException("Unassignable word" + tokens.getFirst());
        }
    }
}
