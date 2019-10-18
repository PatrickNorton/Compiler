package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class EscapedOperatorNode implements NameNode {
    private LineInfo lineInfo;
    private OpFuncTypeNode operator;

    @Contract(pure = true)
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

    @NotNull
    @Contract("_ -> new")
    public static NameNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OP_FUNC);
        LineInfo info = tokens.lineInfo();
        OpFuncTypeNode operator = OpFuncTypeNode.parse(tokens);
        EscapedOperatorNode escaped = new EscapedOperatorNode(info, operator);
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, escaped);
        } else if (tokens.tokenIs("(")) {
            return new FunctionCallNode(escaped, ArgumentNode.parseList(tokens));
        }
        return escaped;
    }
}
