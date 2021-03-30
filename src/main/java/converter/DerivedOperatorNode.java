package main.java.converter;

import main.java.parser.IndependentNode;
import main.java.parser.LineInfo;
import main.java.parser.TestNode;

public final class DerivedOperatorNode implements IndependentNode {
    private final TestNode operator;
    private final LineInfo lineInfo;

    public DerivedOperatorNode(LineInfo lineInfo, TestNode operator) {
        this.lineInfo = lineInfo;
        this.operator = operator;
    }

    public TestNode getOperator() {
        return operator;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }
}
