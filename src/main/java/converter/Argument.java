package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.Objects;

public final class Argument implements Lined {
    private final LineInfo lineInfo;
    private final String name;
    private final TypeObject type;
    private final boolean isVararg;

    public Argument(String name, TypeObject type) {
        this.name = name;
        this.type = type;
        this.isVararg = false;
        this.lineInfo = LineInfo.empty();
    }

    public Argument(String name, TypeObject type, boolean isVararg, LineInfo lineInfo) {
        this.name = name;
        this.type = type;
        this.isVararg = isVararg;
        this.lineInfo = lineInfo;
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
    public LineInfo getLineInfo() {
        return lineInfo;
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

    public static TypeObject[] typesOf(Argument... args) {
        TypeObject[] result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = args[i].getType();
        }
        return result;
    }
}
