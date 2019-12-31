package main.java.converter;

public final class Argument {
    private final String name;
    private final TypeObject type;

    public Argument(String name, TypeObject type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }
}
