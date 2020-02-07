package main.java.converter;

import main.java.parser.NumberNode;
import main.java.parser.StringNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;

public interface LangConstant extends LangObject {
    List<Byte> toBytes();

    TypeObject getType();

    @NotNull
    @Contract("_ -> new")
    static LangConstant of(@NotNull StringNode node) {
        return new StringConstant(node.getContents());
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
            var bigInt = value.toBigIntegerExact();
            // If the integer is small enough to fit into an int, do so
            if (bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                    && bigInt.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
                return new IntConstant(bigInt.intValueExact());
            } else {
                return new BigintConstant(bigInt);
            }
        } else {
            return new DecimalConstant(value);
        }
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static LangConstant of(int value) {
        return new IntConstant(value);
    }
}
