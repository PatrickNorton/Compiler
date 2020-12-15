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

    public static Optional<BigInteger> convertConst(LangConstant value) {
        if (value instanceof IntConstant) {
            return Optional.of(BigInteger.valueOf(((IntConstant) value).getValue()));
        } else if (value instanceof BigintConstant) {
            return Optional.of(((BigintConstant) value).getValue());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Integer> convertToInt(LangConstant value) {
        if (value instanceof IntConstant) {
            return Optional.of(((IntConstant) value).getValue());
        } else if (value instanceof BigintConstant) {
            var intVal = ((BigintConstant) value).getValue();
            if (Util.fitsInInt(intVal)) {
                return Optional.of(intVal.intValueExact());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

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
            case BITWISE_AND:
                return Optional.of(LangConstant.of(and(values)));
            case BITWISE_OR:
                return Optional.of(LangConstant.of(or(values)));
            case BITWISE_XOR:
                return Optional.of(LangConstant.of(xor(values)));
            case BITWISE_NOT:
                if (values.length == 1) {
                    return Optional.of(LangConstant.of(values[0].not()));
                } else {
                    return Optional.empty();
                }
            case MODULO:
                if (values.length == 2) {
                    return Optional.of(LangConstant.of(values[0].mod(values[1])));
                } else {
                    return Optional.empty();
                }
            case POWER:
                return pow(values).map(LangConstant::of);
            case GREATER_THAN:
                return Optional.of(LangConstant.of(gt(values)));
            case LESS_THAN:
                return Optional.of(LangConstant.of(lt(values)));
            case GREATER_EQUAL:
                return Optional.of(LangConstant.of(ge(values)));
            case LESS_EQUAL:
                return Optional.of(LangConstant.of(le(values)));
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
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.subtract(values[i]);
        }
        return result;
    }

    private static BigInteger prod(BigInteger... values) {
        var result = BigInteger.ONE;
        for (var value : values) {
            result = result.multiply(value);
        }
        return result;
    }

    private static BigInteger quot(BigInteger... values) {
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.divide(values[i]);
        }
        return result;
    }

    private static Optional<BigInteger> pow(BigInteger... values) {
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            var value = values[i];
            if (Util.fitsInInt(value)) {
                result = result.pow(value.intValueExact());
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }

    private static BigInteger and(BigInteger... values) {
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.and(values[i]);
        }
        return result;
    }

    private static BigInteger or(BigInteger... values) {
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.or(values[i]);
        }
        return result;
    }

    private static BigInteger xor(BigInteger... values) {
        var result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.xor(values[i]);
        }
        return result;
    }

    private static boolean gt(BigInteger... values) {
        for (int i = 0; i < values.length - 1; i++) {
            if (!(values[i].compareTo(values[i + 1]) > 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean ge(BigInteger... values) {
        for (int i = 0; i < values.length - 1; i++) {
            if (!(values[i].compareTo(values[i + 1]) >= 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean lt(BigInteger... values) {
        for (int i = 0; i < values.length - 1; i++) {
            if (!(values[i].compareTo(values[i + 1]) < 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean le(BigInteger... values) {
        for (int i = 0; i < values.length - 1; i++) {
            if (!(values[i].compareTo(values[i + 1]) <= 0)) {
                return false;
            }
        }
        return true;
    }
}
