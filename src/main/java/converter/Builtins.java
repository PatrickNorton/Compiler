package main.java.converter;

import main.java.parser.OpSpTypeNode;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final TypeObject CALLABLE = new DefaultInterface("Callable", OpSpTypeNode.CALL);

    public static final TypeObject INT = new StdTypeObject("int");

    public static final TypeObject STR = new StdTypeObject("str");

    public static final TypeObject DECIMAL = new StdTypeObject("dec");
}
