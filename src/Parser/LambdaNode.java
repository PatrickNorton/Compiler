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
    private StatementBodyNode body;

    /**
     * Create a new instance of LambdaNode
     * @param args The arguments passed to the lambda
     * @param body The body of the lambda
     */
    @Contract(pure = true)
    public LambdaNode(LineInfo lineInfo, TypedArgumentListNode args, StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.args = args;
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

    /**
     * Parse a new LambdaNode from a list of tokens.
     * <p>
     *     The syntax for a lambda is: <code>"lambda" {@link
     *     TypedArgumentListNode} {@link StatementBodyNode}</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The newly parsed lambda
     */
    @NotNull
    @Contract("_ -> new")
    static LambdaNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.LAMBDA);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        TypedArgumentListNode args = TypedArgumentListNode.parseOnOpenBrace(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new LambdaNode(lineInfo, args, body);
    }

    @Override
    public String toString() {
        return "lambda " + args + body;
    }
}
