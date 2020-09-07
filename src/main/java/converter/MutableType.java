package main.java.converter;

import main.java.parser.DescriptorNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum MutableType {
    STANDARD(""),
    MUT("mut"),
    FINAL("final"),
    MREF("mref"),
    MUT_METHOD("mut"),
    ;

    public final String name;

    MutableType(String name) {
        this.name = name;
    }

    private static final Map<DescriptorNode, MutableType> DESCRIPTOR_MAP = new EnumMap<>(DescriptorNode.class);

    static {
        DESCRIPTOR_MAP.put(DescriptorNode.MUT, MutableType.MUT);
        DESCRIPTOR_MAP.put(DescriptorNode.FINAL, MutableType.FINAL);
        DESCRIPTOR_MAP.put(DescriptorNode.MREF, MutableType.MREF);
    }

    @Contract(pure = true)
    public static MutableType fromDescriptor(@NotNull DescriptorNode descriptor) {
        switch (descriptor) {
            case MUT:
                return MUT;
            case FINAL:
                return FINAL;
            case MREF:
                return MREF;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static MutableType fromDescriptors(Set<DescriptorNode> descriptors) {
        for (var pair : DESCRIPTOR_MAP.entrySet()) {
            if (descriptors.contains(pair.getKey())) {
                return pair.getValue();
            }
        }
        return MutableType.STANDARD;
    }
}
