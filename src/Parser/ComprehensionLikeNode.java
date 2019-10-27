package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class ComprehensionLikeNode implements SubTestNode, PostDottableNode {
    private LineInfo lineInfo;
    private String brace;
    private TypedVariableNode[] variables;
    private TestNode builder;
    private TestNode[] looped;
    private TestNode condition;

    @Contract(pure = true)
    public ComprehensionLikeNode(LineInfo lineInfo, String brace, TypedVariableNode[] variables,
                                 TestNode builder, TestNode[] looped, TestNode condition) {
        this.lineInfo = lineInfo;
        this.brace = brace;
        this.variables = variables;
        this.builder = builder;
        this.looped = looped;
        this.condition = condition;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getBrace() {
        return brace;
    }

    public TestNode getBuilder() {
        return builder;
    }

    public TestNode getCondition() {
        return condition;
    }

    public TestNode[] getLooped() {
        return looped;
    }

    public TypedVariableNode[] getVariables() {
        return variables;
    }

    @NotNull
    static ComprehensionLikeNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        switch (tokens.tokenSequence()) {
            case "{":
                if (tokens.braceContains(":")) {
                    return DictComprehensionNode.parse(tokens);
                }
            case "(":
            case "[":
                return ComprehensionNode.parse(tokens);
            default:
                throw tokens.internalError("Unknown brace type " + tokens.getFirst());
        }
    }

    String secondHalfString() {
        String variables = TestNode.toString(this.variables);
        String looped = TestNode.toString(this.looped);
        String condition = this.condition.isEmpty() ? "" : " if " + this.condition;
        return String.format(" for %s in %s%s%s", variables, looped,
                condition, TokenList.matchingBrace(brace));
    }

    @Override
    public String toString() {
        return brace + builder + secondHalfString();
    }
}
