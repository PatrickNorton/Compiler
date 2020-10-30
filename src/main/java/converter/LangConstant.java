package main.java.converter;

import main.java.parser.NumberNode;
import main.java.parser.StringNode;
import main.java.util.IndexedSet;
import main.java.util.OptionalBool;

import java.math.BigInteger;
import java.util.List;

public interface LangConstant extends LangObject {

    List<Byte> toBytes();

    TypeObject getType();

    default String name(IndexedSet<LangConstant> constants) {
        return "";
    }

    default OptionalBool boolValue() {
        return OptionalBool.empty();
    }

    static LangConstant of(StringNode node) {
        return new StringConstant(node.getContents());
    }

    static LangConstant of(String value) {
        return new StringConstant(value);
    }

    static LangConstant of(NumberNode node) {
        var value = node.getValue();
        // If the value is an integer, e.g. 6
        if (value.scale() == 0 || value.stripTrailingZeros().scale() == 0) {
            var bigInt = value.toBigIntegerExact();
            // If the integer is small enough to fit into an int, do so
            if (Util.fitsInInt(bigInt)) {
                return new IntConstant(bigInt.intValueExact());
            } else {
                return new BigintConstant(bigInt);
            }
        } else {
            return new DecimalConstant(value);
        }
    }
    
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
