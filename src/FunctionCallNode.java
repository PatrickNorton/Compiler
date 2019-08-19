public class FunctionCallNode implements NameNode {
    private TestNode caller;
    private ArgumentNode[] parameters;

    public FunctionCallNode(TestNode caller, ArgumentNode[] parameters) {
        this.caller = caller;
        this.parameters = parameters;
    }

    public TestNode getCaller() {
        return caller;
    }

    public ArgumentNode[] getParameters() {
        return parameters;
    }
}
