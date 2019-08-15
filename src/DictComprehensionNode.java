public class DictComprehensionNode implements SubTestNode {
    private TestNode key;
    private TestNode val;
    private TypedVariableNode[] vars;
    private TestNode[] looped;

    public DictComprehensionNode(TestNode key, TestNode val, TypedVariableNode[] vars, TestNode[] looped) {
        this.key = key;
        this.val = val;
        this.vars = vars;
        this.looped = looped;
    }

    public TestNode getKey() {
        return key;
    }

    public TestNode getVal() {
        return val;
    }

    public TypedVariableNode[] getVars() {
        return vars;
    }

    public TestNode[] getLooped() {
        return looped;
    }
}
