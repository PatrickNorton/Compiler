public class LambdaNode implements SubTestNode {
    private TypedArgumentListNode args;
    private StatementBodyNode body;

    public LambdaNode(TypedArgumentListNode args, StatementBodyNode body) {
        this.args = args;
        this.body = body;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    static LambdaNode parse(TokenList tokens) {
        assert tokens.tokenIs("lambda");
        tokens.nextToken();
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new LambdaNode(args, body);
    }
}
