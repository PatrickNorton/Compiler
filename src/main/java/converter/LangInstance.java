package main.java.converter;

public final class LangInstance implements LangObject {
    private final TypeObject type;

    public LangInstance(TypeObject type) {
        this.type = type;
    }

    @Override
    public TypeObject getType() {
        return type;
    }
}
