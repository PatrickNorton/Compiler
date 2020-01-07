package main.java.converter;

public final class VariableInfo {
    private final TypeObject type;
    private final boolean isConst;
    private final LangConstant langConst;

    public VariableInfo(TypeObject type) {
        this(type, false);
    }

    public VariableInfo(TypeObject type, boolean isConst) {
        this.type = type;
        this.isConst = isConst;
        this.langConst = null;
    }

    public VariableInfo(TypeObject type, LangConstant constValue) {
        this.type = type;
        this.isConst = true;
        this.langConst = constValue;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isConst() {
        return isConst;
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
