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
}
