package main.java.converter;

public final class StandardArgPos implements ArgPosition {
    private final int value;

    public StandardArgPos(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
