package main.java.parser;

public class EscapedOperatorNode implements NameNode {
    private LineInfo lineInfo;
    private OpFuncTypeNode operator;

    public EscapedOperatorNode(LineInfo lineInfo, OpFuncTypeNode operator) {
        this.lineInfo = lineInfo;
        this.operator = operator;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public OpFuncTypeNode getOperator() {
        return operator;
    }

    public static NameNode parse(TokenList tokens, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.OP_FUNC);
        LineInfo info = tokens.lineInfo();
        OpFuncTypeNode operator = OpFuncTypeNode.parse(tokens);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        return new EscapedOperatorNode(info, operator);
    }

    @Override
    public String toString() {
        return operator.toString();
    }
}
