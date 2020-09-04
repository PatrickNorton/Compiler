package main.java.converter;

import main.java.parser.DescriptorNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum AccessLevel {
    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBGET("pubget"),
    FILE("file"),
    ;

    public final String name;

    AccessLevel(String name) {
        this.name = name;
    }

    private static final Map<DescriptorNode, AccessLevel> DESCRIPTOR_MAP = new EnumMap<>(DescriptorNode.class);

    static {
        DESCRIPTOR_MAP.put(DescriptorNode.PUBLIC, AccessLevel.PUBLIC);
        DESCRIPTOR_MAP.put(DescriptorNode.PRIVATE,  AccessLevel.PRIVATE);
        DESCRIPTOR_MAP.put(DescriptorNode.PROTECTED, AccessLevel.PROTECTED);
        DESCRIPTOR_MAP.put(DescriptorNode.PUBGET, AccessLevel.PUBGET);
    }

    public static AccessLevel fromDescriptors(Set<DescriptorNode> descriptors) {
        for (var pair : DESCRIPTOR_MAP.entrySet()) {
            if (descriptors.contains(pair.getKey())) {
                return pair.getValue();
            }
        }
        return AccessLevel.FILE;
    }

    // FIXME: Here for compatibility reasons, remove as soon as possible
    public static boolean canAccess(AccessLevel valueLevel, DescriptorNode accessLevel) {
        return canAccess(valueLevel, DESCRIPTOR_MAP.get(accessLevel));
    }

    @Contract(pure = true)
    public static boolean canAccess(@NotNull AccessLevel valueLevel, AccessLevel accessLevel) {
        switch (valueLevel) {
            case PUBLIC:
                return true;
            case PRIVATE:
                return accessLevel == PRIVATE;
            case PUBGET:
                return true;  // TODO: Mutable/immutable access
            case PROTECTED:
                return accessLevel == PRIVATE || accessLevel == PROTECTED;
            case FILE:
                return accessLevel == PRIVATE || accessLevel == FILE;
            default:
                throw new IllegalStateException("Unknown AccessLevel");
        }
    }
}
