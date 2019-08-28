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

    static DictComprehensionNode parse(TokenList tokens) {  // REFACTORED: DictComprehensionNode.parse
        assert tokens.tokenIs("{");
        tokens.nextToken(true);
        TestNode key = TestNode.parse(tokens, true);
        if (!tokens.tokenIs(":")) {
            throw new ParserException("Expected :, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        TestNode val = TestNode.parse(tokens, true);
        if (!tokens.tokenIs("for")) {
            throw new ParserException("Expected for, got "+tokens.getFirst());
        }
        tokens.nextToken();
        TypedVariableNode[] vars = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Expected in, got "+tokens.getFirst());
        }
        tokens.nextToken();
        TestNode[] looped = TestNode.parseList(tokens, true);
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Expected }, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DictComprehensionNode(key, val, vars, looped);
    }
}
