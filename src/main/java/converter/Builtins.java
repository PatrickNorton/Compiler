package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.Set;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final Set<String> FORBIDDEN_NAMES = Set.of(
            "true",
            "false",
            "__default__"
    );

    public static final TypeObject CALLABLE = new DefaultInterface("Callable", OpSpTypeNode.CALL);

    public static final TypeObject INT = new StdTypeObject("int");

    public static final TypeObject STR = new StdTypeObject("str");

    public static final TypeObject DECIMAL = new StdTypeObject("dec");

    public static final TypeObject TYPE = new TypeTypeObject();
}
