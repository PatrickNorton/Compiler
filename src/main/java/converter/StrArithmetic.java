package main.java.converter;

import main.java.parser.OperatorTypeNode;

import java.math.BigInteger;
import java.util.Optional;

public final class StrArithmetic {
    private StrArithmetic() {}

    public static Optional<LangConstant> computeConst(OperatorTypeNode op, LangConstant[] consts) {
        assert consts[0] instanceof StringConstant;
        switch (op) {
            case ADD:
                return add(consts);
            case MULTIPLY:
                return mul(consts);
            case IS:
            case EQUALS:
                return eq(consts);
            case IS_NOT:
            case NOT_EQUALS:
                return ne(consts);
            default:
                return Optional.empty();
        }
    }

    private static Optional<LangConstant> add(LangConstant[] consts) {
        StringBuilder result = new StringBuilder();
        for (var constant : consts) {
            if (constant instanceof StringConstant) {
                result.append(((StringConstant) constant).getValue());
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(LangConstant.of(result.toString()));
    }

    private static Optional<LangConstant> mul(LangConstant[] consts) {
        String value = ((StringConstant) consts[0]).getValue();
        BigInteger mul = BigInteger.ONE;
        for (int i = 1; i < consts.length; i++) {
            var constant = consts[i];
            if (constant instanceof BigintConstant) {
                mul = mul.multiply(((BigintConstant) constant).getValue());
            } else if (constant instanceof IntConstant) {
                mul = mul.multiply(BigInteger.valueOf(((IntConstant) constant).getValue()));
            } else {
                return Optional.empty();
            }
        }
        if (Util.fitsInInt(mul)) {
            long count = mul.intValueExact();
            if ((value.length() * count) > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            return Optional.of(LangConstant.of(value.repeat((int) count)));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<LangConstant> eq(LangConstant[] consts) {
        String value = ((StringConstant) consts[0]).getValue();
        for (int i = 1; i < consts.length; i++) {
            var constant = consts[i];
            if (constant instanceof StringConstant) {
                if (!value.equals(((StringConstant) constant).getValue())) {
                    return Optional.of(Builtins.FALSE);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(Builtins.TRUE);
    }

    private static Optional<LangConstant> ne(LangConstant[] consts) {
        String value = ((StringConstant) consts[0]).getValue();
        for (int i = 1; i < consts.length; i++) {
            var constant = consts[i];
            if (constant instanceof StringConstant) {
                if (value.equals(((StringConstant) constant).getValue())) {
                    return Optional.of(Builtins.FALSE);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(Builtins.TRUE);
    }
}
