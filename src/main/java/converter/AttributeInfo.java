package main.java.converter;

import main.java.parser.DescriptorNode;

import java.util.Set;

public final class AttributeInfo {
    private final Set<DescriptorNode> descriptors;
    private final TypeObject type;

    public AttributeInfo(Set<DescriptorNode> descriptors, TypeObject info) {
        this.descriptors = descriptors;
        this.type = info;
    }

    public Set<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    public TypeObject getType() {
        return type;
    }
}
