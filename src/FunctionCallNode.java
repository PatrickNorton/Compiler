public class FunctionCallNode implements SubTestNode {
    private VariableNode caller;
    private ArgumentNode[] parameters;

    public FunctionCallNode(VariableNode caller, ArgumentNode[] parameters) {
        this.caller = caller;
        this.parameters = parameters;
    }

    public VariableNode getCaller() {
        return caller;
    }

    public ArgumentNode[] getParameters() {
        return parameters;
    }
}
