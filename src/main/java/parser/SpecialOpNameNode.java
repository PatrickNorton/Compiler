package main.java.parser;

/**
 * The node representing the name of a special operator.
 * @author Patrick Norton
 */
public class SpecialOpNameNode implements NameNode {
    private LineInfo lineInfo;
    private OpSpTypeNode operator;

    public SpecialOpNameNode(LineInfo lineInfo, OpSpTypeNode operator) {
        this.lineInfo = lineInfo;
        this.operator = operator;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public OpSpTypeNode getOperator() {
        return operator;
    }

    /**
     * Parse a SpecialOpNameNode from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed SpecialOpNameNode
     */

    public static SpecialOpNameNode parse(TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        LineInfo lineInfo = tokens.lineInfo();
        OpSpTypeNode op = OpSpTypeNode.parse(tokens);
        return new SpecialOpNameNode(lineInfo, op);
    }

    @Override
    public String toString() {
        return operator.toString();
    }
}
