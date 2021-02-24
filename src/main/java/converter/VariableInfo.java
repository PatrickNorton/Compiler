package main.java.converter;

import main.java.parser.LineInfo;

import java.util.Objects;

public final class VariableInfo {
    private final TypeObject type;
    private final boolean isConst;
    private final boolean isStatic;
    private final LangConstant langConst;
    private final short location;
    private final LineInfo declarationInfo;

    public VariableInfo(TypeObject type, short location, LineInfo info) {
        this(type, false, location, info);
    }

    public VariableInfo(TypeObject type, boolean isConst, short location, LineInfo info) {
        this(type, isConst, false, location, info);
    }

    public VariableInfo(TypeObject type, boolean isConst, boolean isStatic, short location, LineInfo info) {
        this.type = type;
        this.isConst = isConst;
        this.isStatic = isStatic;
        this.langConst = null;
        this.location = location;
        this.declarationInfo = info;
    }

    public VariableInfo(TypeObject type, LangConstant constValue, LineInfo info) {
        this.type = type;
        this.isConst = true;
        this.isStatic = false;
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

    public boolean isStatic() {
        return isStatic;
    }

    public short getLocation() {
        return isStatic ? -1 : location;
    }

    public short getStaticLocation() {
        return isStatic ? location : -1;
    }

    public LangConstant constValue() {
        return Objects.requireNonNull(langConst, "Cannot invoke constValue() where null");
    }

    public boolean hasConstValue() {
        return langConst != null;
    }

    public LineInfo getDeclarationInfo() {
        return declarationInfo;
    }
}
