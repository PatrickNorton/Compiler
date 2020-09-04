package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

public final class AttributeInfo implements Lined, IntoAttrInfo {
    private final AccessLevel accessLevel;
    private final MutableType mutType;
    private final TypeObject type;
    private final LineInfo lineInfo;

    public AttributeInfo(TypeObject type, LineInfo lineInfo) {
        this(AccessLevel.FILE, type, lineInfo);
    }

    public AttributeInfo(AccessLevel accessLevel, TypeObject type) {
        this(accessLevel, MutableType.STANDARD, type);
    }

    public AttributeInfo(AccessLevel accessLevel, MutableType mutType, TypeObject type) {
        this(accessLevel, mutType, type, LineInfo.empty());
    }

    public AttributeInfo(AccessLevel accessLevel, TypeObject type, LineInfo lineInfo) {
        this(accessLevel, MutableType.STANDARD, type, lineInfo);
    }

    public AttributeInfo(AccessLevel accessLevel, MutableType mutType, TypeObject type, LineInfo lineInfo) {
        this.accessLevel = accessLevel;
        this.mutType = mutType;
        this.type = type;
        this.lineInfo = lineInfo;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public MutableType getMutType() {
        return mutType;
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
