package main.java.converter;

import main.java.parser.IndependentNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;

public final class DerivedOperatorNode implements IndependentNode {
    private final OpSpTypeNode operator;
    private final LineInfo lineInfo;

    public DerivedOperatorNode(LineInfo lineInfo, OpSpTypeNode operator) {
        this.lineInfo = lineInfo;
        this.operator = operator;
    }

    public OpSpTypeNode getOperator() {
        return operator;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }
}
