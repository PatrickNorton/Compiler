package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.TestNode;

public final class VariantCreationNode implements TestNode {
    private final LineInfo lineInfo;
    private final TestNode union;
    private final String variantName;
    private final int variantNo;
    private final TestNode value;

    public VariantCreationNode(LineInfo lineInfo, TestNode union, String variantName, int variantNo, TestNode value) {
        this.lineInfo = lineInfo;
        this.union = union;
        this.variantName = variantName;
        this.variantNo = variantNo;
        this.value = value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getUnion() {
        return union;
    }

    public int getVariantNo() {
        return variantNo;
    }

    public String getVariantName() {
        return variantName;
    }

    public TestNode getValue() {
        return value;
    }
}
