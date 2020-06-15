package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.Collections;
import java.util.Set;

public final class AttributeInfo implements Lined, IntoAttrInfo {
    private final Set<DescriptorNode> descriptors;
    private final TypeObject type;
    private final LineInfo lineInfo;

    public AttributeInfo(TypeObject type, LineInfo lineInfo) {
        this(Collections.emptySet(), type, lineInfo);
    }

    public AttributeInfo(Set<DescriptorNode> descriptors, TypeObject info) {
        this.descriptors = descriptors;
        this.type = info;
        this.lineInfo = LineInfo.empty();
    }

    public AttributeInfo(Set<DescriptorNode> descriptors, TypeObject info, LineInfo lineInfo) {
        this.descriptors = descriptors;
        this.type = info;
        this.lineInfo = lineInfo;
    }

    public Set<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    public TypeObject getType() {
        return type;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public AttributeInfo intoAttrInfo() {
        return this;
    }
}
