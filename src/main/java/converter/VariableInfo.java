package main.java.converter;

public final class VariableInfo {
    private final TypeObject type;
    private final boolean isConst;
    private final LangConstant langConst;
    private final int location;

    public VariableInfo(TypeObject type, int location) {
        this(type, false, location);
    }

    public VariableInfo(TypeObject type, boolean isConst, int location) {
        this.type = type;
        this.isConst = isConst;
        this.langConst = null;
        this.location = location;
    }

    public VariableInfo(TypeObject type, LangConstant constValue, int location) {
        this.type = type;
        this.isConst = true;
        this.langConst = constValue;
        this.location = location;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isConst() {
        return isConst;
    }

    public int getLocation() {
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
