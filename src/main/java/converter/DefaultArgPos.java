package main.java.converter;

public final class DefaultArgPos implements ArgPosition {
    private final Argument.DefaultValue value;

    public DefaultArgPos(Argument.DefaultValue value) {
        this.value = value;
    }

    public Argument.DefaultValue getValue() {
        return value;
    }
}
