package main.java.converter;

import main.java.parser.LineInfo;

public final class VariableInfo {
    private final TypeObject type;
    private final boolean isConst;
    private final LangConstant langConst;
    private final short location;
    private final LineInfo declarationInfo;

    public VariableInfo(TypeObject type, short location, LineInfo info) {
        this(type, false, location, info);
    }

    public VariableInfo(TypeObject type, boolean isConst, short location, LineInfo info) {
        this.type = type;
        this.isConst = isConst;
        this.langConst = null;
        this.location = location;
        this.declarationInfo = info;
    }

    public VariableInfo(TypeObject type, LangConstant constValue, LineInfo info) {
        this.type = type;
        this.isConst = true;
        this.langConst = constValue;
        this.location = -1;
        this.declarationInfo = info;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isConst() {
        return isConst;
    }

    public short getLocation() {
        return location;
    }

    public LangConstant constValue() {
        if (langConst == null) {
            throw new IllegalStateException("Cannot invoke constValue() where null");
        } else {
            return langConst;
        }
    }

    public boolean hasConstValue() {
        return langConst != null;
    }

    public LineInfo getDeclarationInfo() {
        return declarationInfo;
    }
}
