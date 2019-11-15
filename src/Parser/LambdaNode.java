package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a lambda statement.
 * @author Patrick Norton
 */
public class LambdaNode implements SubTestNode {
    private LineInfo lineInfo;
    private TypedArgumentListNode args;
    private TypeLikeNode[] returns;
    private boolean isArrow;
    private StatementBodyNode body;

    /**
     * Create a new instance of LambdaNode
     * @param args The arguments passed to the lambda
     * @param body The body of the lambda
     */
    @Contract(pure = true)
    public LambdaNode(LineInfo lineInfo, TypedArgumentListNode args, TypeLikeNode[] returns, boolean isArrow, StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.args = args;
        this.returns = returns;
        this.isArrow = isArrow;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeLikeNode[] getReturns() {
        return returns;
    }

    public boolean isArrow() {
        return isArrow;
    }

    /**
     * Parse a new LambdaNode from a list of tokens.
     * <p>
     *     The syntax for a lambda is: <code>"lambda" {@link
     *     TypedArgumentListNode} {@link StatementBodyNode}</code>. Unlike
     *     other {@link TypedArgumentListNode}s, this one does not have to be
     *     typed, as that can be inferred dynamically in some cases.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The newly parsed lambda
     */
    @NotNull
    @Contract("_, _ -> new")
    static LambdaNode parse(@NotNull TokenList tokens, boolean ignoreNewlines) {
        assert tokens.tokenIs(Keyword.LAMBDA);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken(ignoreNewlines);
        TypedArgumentListNode args = TypedArgumentListNode.parseOptionalParens(tokens);
        TypeLikeNode[] returns = TypeLikeNode.parseRetVal(tokens, ignoreNewlines);
        boolean isArrow;
        StatementBodyNode body;
        if (tokens.tokenIs(TokenType.DOUBLE_ARROW)) {
            isArrow = true;
            LineInfo info = tokens.lineInfo();
            tokens.nextToken(ignoreNewlines);
            body = new StatementBodyNode(info, TestNode.parse(tokens, ignoreNewlines));
        } else {
            isArrow = false;
            body = StatementBodyNode.parse(tokens);
        }
        return new LambdaNode(lineInfo, args, returns, isArrow, body);
    }

    @Override
    public String toString() {
        return String.format("lambda %s%s %s", args, TypeLikeNode.returnString(returns), isArrow ? arrowString() : body);
    }

    private String arrowString() {
        return String.format("=> %s", body.get(0));
    }
}
