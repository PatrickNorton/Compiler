package main.java.converter;

public final class VariableInfo {
    private final TypeObject type;
    private final boolean isConst;
    private final LangConstant langConst;
    private final short location;

    public VariableInfo(TypeObject type, short location) {
        this(type, false, location);
    }

    public VariableInfo(TypeObject type, boolean isConst, short location) {
        this.type = type;
        this.isConst = isConst;
        this.langConst = null;
        this.location = location;
    }

    public VariableInfo(TypeObject type, LangConstant constValue) {
        this.type = type;
        this.isConst = true;
        this.langConst = constValue;
        this.location = -1;
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
}
