package main.java.converter;

import main.java.parser.OpSpTypeNode;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final TypeObject CALLABLE = new DefaultInterface("Callable", OpSpTypeNode.CALL);
}
