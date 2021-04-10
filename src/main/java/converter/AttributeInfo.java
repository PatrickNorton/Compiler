package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

public final class AttributeInfo implements Lined, IntoAttrInfo {
    private final boolean isMethod;
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

    public AttributeInfo(boolean isMethod, AccessLevel accessLevel, MutableType mutType, TypeObject type) {
        this(isMethod, accessLevel, mutType, type, LineInfo.empty());
    }

    public AttributeInfo(AccessLevel accessLevel, TypeObject type, LineInfo lineInfo) {
        this(accessLevel, MutableType.STANDARD, type, lineInfo);
    }

    public AttributeInfo(AccessLevel accessLevel, MutableType mutType, TypeObject type, LineInfo lineInfo) {
        this(false, accessLevel, mutType, type, lineInfo);
    }

    public AttributeInfo(
            boolean isMethod, AccessLevel accessLevel, MutableType mutType, TypeObject type, LineInfo lineInfo
    ) {
        this.isMethod = isMethod;
        this.accessLevel = accessLevel;
        this.mutType = mutType;
        this.type = type;
        this.lineInfo = lineInfo;
    }

    public static AttributeInfo method(FunctionInfo info) {
        return new AttributeInfo(true, AccessLevel.PUBLIC, MutableType.STANDARD, info.toCallable());
    }

    public static AttributeInfo mutMethod(FunctionInfo info) {
        return new AttributeInfo(true, AccessLevel.PUBLIC, MutableType.MUT_METHOD, info.toCallable());
    }

    public boolean isMethod() {
        return isMethod;
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
