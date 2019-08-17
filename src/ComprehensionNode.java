public class ComprehensionNode implements SubTestNode {
    private String brace_type;
    private TypedVariableNode[] variables;
    private TestNode builder;
    private TestNode[] looped;

    public ComprehensionNode(String brace_type, TypedVariableNode[] variables, TestNode builder, TestNode[] looped) {
        this.brace_type = brace_type;
        this.variables = variables;
        this.builder = builder;
        this.looped = looped;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TypedVariableNode[] getVariables() {
        return variables;
    }

    public TestNode getBuilder() {
        return builder;
    }

    public TestNode[] getLooped() {
        return looped;
    }

    public boolean hasBraces() {
        return !brace_type.isEmpty();
    }
}
