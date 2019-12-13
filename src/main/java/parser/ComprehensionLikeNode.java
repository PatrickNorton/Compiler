package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class ComprehensionLikeNode implements SubTestNode, PostDottableNode {
    private LineInfo lineInfo;
    private String brace;
    private VarLikeNode[] variables;
    private ArgumentNode[] builder;
    private TestListNode looped;
    private TestNode condition;
    private TestNode whileCond;

     public ComprehensionLikeNode(LineInfo lineInfo, String brace, VarLikeNode[] variables,
                                 TestNode builder, TestListNode looped, TestNode condition, TestNode whileCond) {
         this(lineInfo, brace, variables, ArgumentNode.fromTestNodes(builder), looped, condition, whileCond);
     }

    @Contract(pure = true)
    public ComprehensionLikeNode(LineInfo lineInfo, String brace, VarLikeNode[] variables,
                                 ArgumentNode[] builder, TestListNode looped, TestNode condition, TestNode whileCond) {
        this.lineInfo = lineInfo;
        this.brace = brace;
        this.variables = variables;
        this.builder = builder;
        this.looped = looped;
        this.condition = condition;
        this.whileCond = whileCond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getBrace() {
        return brace;
    }

    public ArgumentNode[] getBuilder() {
        return builder;
    }

    public TestNode getCondition() {
        return condition;
    }

    public TestListNode getLooped() {
        return looped;
    }

    public VarLikeNode[] getVariables() {
        return variables;
    }

    public TestNode getWhileCond() {
        return whileCond;
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
        String condition = this.condition.isEmpty() ? "" : " if " + this.condition;
        String whileCond = this.whileCond.isEmpty() ? "" : " while " + this.whileCond;
        return String.format(" for %s in %s%s%s%s", variables, looped,
                condition, whileCond, TokenList.matchingBrace(brace));
    }

    @Override
    public String toString() {
        return brace + ArgumentNode.toString(builder) + secondHalfString();
    }
}
