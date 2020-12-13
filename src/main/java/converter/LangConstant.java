package main.java.converter;

import main.java.parser.NumberNode;
import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

public interface LangConstant extends LangObject {
    @NotNull
    List<Byte> toBytes();

    @NotNull
    TypeObject getType();

    @NotNull
    default String name(IndexedSet<LangConstant> constants) {
        return "";
    }

    default OptionalBool boolValue() {
        return OptionalBool.empty();
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static LangConstant of(String value) {
        return new StringConstant(value);
    }

    @NotNull
    @Contract("_ -> new")
    static LangConstant of(@NotNull NumberNode node) {
        var value = node.getValue();
        // If the value is an integer, e.g. 6
        if (value.scale() == 0 || value.stripTrailingZeros().scale() == 0) {
            return of(value.toBigIntegerExact());
        } else {
            return new DecimalConstant(value);
        }
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static LangConstant of(int value) {
        return new IntConstant(value);
    }

    static LangConstant of(BigInteger value) {
        if (Util.fitsInInt(value)) {
            return new IntConstant(value.intValueExact());
        } else {
            return new BigintConstant(value);
        }
    }

    static LangConstant of(boolean value) {
        return value ? Builtins.TRUE : Builtins.FALSE;
    }
}
