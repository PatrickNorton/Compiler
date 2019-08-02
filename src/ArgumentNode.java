public class ArgumentNode implements BaseNode {
    private VariableNode variable;
    private String vararg;
    private TestNode argument;

    public ArgumentNode(VariableNode variable, String vararg, TestNode argument) {
        this.variable = variable;
        this.vararg = vararg;
        this.argument = argument;
    }

    public VariableNode getVariable() {
        return variable;
    }

    public String getVararg() {
        return vararg;
    }

    public TestNode getArgument() {
        return argument;
    }

    public boolean isVararg() {
        return !vararg.isEmpty();
    }
}
