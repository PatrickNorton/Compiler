package main.java.converter;

import main.java.parser.OperatorTypeNode;

import java.math.BigInteger;
import java.util.Optional;

/**
 * The class for computing compile-time integer arithmetic.
 *
 * @author Patrick Norton
 */
public final class IntArithmetic {
    private IntArithmetic() {}

    public static Optional<LangConstant> computeConst(OperatorTypeNode op, BigInteger[] values) {
        switch (op) {
            case ADD:
                return Optional.of(LangConstant.of(sum(values)));
            case SUBTRACT:
                return Optional.of(LangConstant.of(diff(values)));
            case U_SUBTRACT:
                if (values.length == 1) {
                    return Optional.of(LangConstant.of(values[0].negate()));
                } else {
                    return Optional.empty();
                }
            case MULTIPLY:
                return Optional.of(LangConstant.of(prod(values)));
            case DIVIDE:
                return Optional.of(LangConstant.of(quot(values)));
            case LEFT_BITSHIFT:
                if (values.length == 2 && Util.fitsInInt(values[1])) {
                    return Optional.of(LangConstant.of(values[0].shiftLeft(values[1].intValueExact())));
                } else {
                    return Optional.empty();
                }
            case RIGHT_BITSHIFT:
                if (values.length == 2 && Util.fitsInInt(values[1])) {
                    return Optional.of(LangConstant.of(values[0].shiftRight(values[1].intValueExact())));
                } else {
                    return Optional.empty();
                }
            case EQUALS:
            case IS:
                return Optional.of(LangConstant.of(eq(values)));
            case NOT_EQUALS:
            case IS_NOT:
                if (values.length == 2) {
                    return Optional.of(LangConstant.of(!values[0].equals(values[1])));
                } else {
                    return Optional.empty();
                }
            default:
                return Optional.empty();
        }
    }

    private static boolean eq(BigInteger... values) {
        if (values.length == 0) {
            return true;
        }
        for (var value : values) {
            if (!value.equals(values[0])) {
                return false;
            }
        }
        return true;
    }

    private static BigInteger sum(BigInteger... values) {
        var result = BigInteger.ZERO;
        for (var value : values) {
            result = result.add(value);
        }
        return result;
    }

    private static BigInteger diff(BigInteger... values) {
        var result = BigInteger.ZERO;
        for (var value : values) {
            result = result.subtract(value);
        }
        return result;
    }

    private static BigInteger prod(BigInteger... values) {
        var result = BigInteger.ZERO;
        for (var value : values) {
            result = result.multiply(value);
        }
        return result;
    }

    private static BigInteger quot(BigInteger... values) {
        var result = BigInteger.ZERO;
        for (var value : values) {
            result = result.divide(value);
        }
        return result;
    }
}
