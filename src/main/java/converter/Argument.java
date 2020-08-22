package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Argument {
    private final String name;
    private final TypeObject type;
    private final boolean isVararg;

    public Argument(String name, TypeObject type) {
        this.name = name;
        this.type = type;
        this.isVararg = false;
    }

    public Argument(String name, TypeObject type, boolean isVararg) {
        this.name = name;
        this.type = type;
        this.isVararg = isVararg;
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isVararg() {
        return isVararg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return Objects.equals(name, argument.name) &&
                Objects.equals(type, argument.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @NotNull
    @Contract(pure = true)
    public static TypeObject[] typesOf(@NotNull Argument... args) {
        TypeObject[] result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = args[i].getType();
        }
        return result;
    }
}
