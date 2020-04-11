package main.java.converter;

public class LangInstance implements LangObject {
    private TypeObject type;

    public LangInstance(TypeObject type) {
        this.type = type;
    }

    @Override
    public TypeObject getType() {
        return type;
    }
}
