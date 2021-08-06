package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NormalOperatorConverter extends OperatorConverter {
    private final OperatorTypeNode op;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public NormalOperatorConverter(
            OperatorTypeNode op,
            ArgumentNode[] args,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.op = op;
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        return defaultConstant(op, info, args);
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        var firstOpConverter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = firstOpConverter.returnType()[0].operatorReturnType(op, info);
        return retType.orElseGet(() -> new TypeObject[]{Builtins.throwsType()});
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        if (op == OperatorTypeNode.NOT_EQUALS) {
            return convertNotEquals(start);
        }
        var opt = optimizeConstant(start);
        if (opt.isPresent()) {
            return opt.orElseThrow();
        }
        return convertInner(start);
    }

    @NotNull
    private List<Byte> convertInner(int start) {
        assert op != OperatorTypeNode.NOT_EQUALS;
        List<Byte> bytes = new ArrayList<>();
        int opCount = args.length;
        TypeObject opType = null;
        ArgumentNode previousArg = null;
        for (var arg : args) {
            var converter = TestConverter.of(info, arg.getArgument(), 1);
            var retTypes = converter.returnType();
            if (retTypes.length == 0) {
                throw CompilerException.of("Cannot use return type of function with 0 returns", arg);
            }
            var retType = retTypes[0];
            if (opType != null && opType.operatorReturnType(op, info).isEmpty()) {
                throw CompilerException.format(
                        "'%s' returns type '%s', which has no overloaded '%s'",
                        previousArg, previousArg, opType.name(), op
                );
            }
            opType = opType == null ? retType : opType.operatorReturnType(op, info).orElseThrow()[0];
            previousArg = arg;
            bytes.addAll(TestConverter.bytes(start + bytes.size(), arg.getArgument(), info, 1));
        }
        var bytecode = BYTECODE_MAP.get(op);
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(bytecode.value);
        } else if (MANDATORY_ARG_COUNT.contains(op)) {
            throw CompilerException.format(
                    "Cannot call operator '%s' with %d operands (expected exactly %d)",
                    lineInfo, op, opCount, op.isUnary() ? 1 : 2
            );
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertNotEquals(int start) {
        int opCount = args.length;
        assert opCount == 2 && op == OperatorTypeNode.NOT_EQUALS;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), args[1].getArgument(), info, 1));
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(Bytecode.EQUAL.value);
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        bytes.add(Bytecode.BOOL_NOT.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private Optional<List<Byte>> optimizeConstant(int start) {
        if (args.length != 2) {
            return Optional.empty();
        }
        var firstConverter = TestConverter.of(info, args[0].getArgument(), 1);
        var secondConverter = TestConverter.of(info, args[1].getArgument(), 1);
        if (!Builtins.intType().isSuperclass(firstConverter.returnType()[0])
            || !Builtins.intType().isSuperclass(secondConverter.returnType()[0])) {
            return Optional.empty();
        }
        var secondConstant = secondConverter.constantReturn();
        if (secondConstant.isPresent()) {
            return Optional.of(secondConstant(firstConverter, (NumberConstant) secondConstant.orElseThrow(), start));
        }
        var firstConstant = firstConverter.constantReturn();
        if (firstConstant.isPresent()) {
            return Optional.of(firstConstant(firstConverter, (NumberConstant) firstConstant.orElseThrow(), start));
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        throw asException(lineInfo);
    }

    private List<Byte> secondConstant(TestConverter converter, NumberConstant constant, int start) {
        switch (op) {
            case ADD:
                return convertAdditionConstant(converter, constant, start);
            case SUBTRACT:
                return convertSubtractionConstant(converter, constant, start);
            case MULTIPLY:
                return convertMultiplicationConstant(converter, constant, start);
            case DIVIDE:
                return convertDivisionConstant(converter, constant, start);
            case FLOOR_DIV:
                return convertFloorDivConstant(converter, constant, start);
            case POWER:
                return convertPowConstant(converter, constant, start);
            case LEFT_BITSHIFT:
                return convertLShiftConstant(converter, constant, start);
            case RIGHT_BITSHIFT:
                return convertRShiftConstant(converter, constant, start);
            case BITWISE_AND:
                return convertAndConstant(converter, constant, start);
            case BITWISE_OR:
                return convertOrConstant(converter, constant, start);
            case BITWISE_XOR:
                return convertXorConstant(converter, constant, start);
            case MODULO:
                return convertModConstant(converter, constant, start);
            default:
                return convertOneConstant(converter, constant, start);
        }
    }

    private List<Byte> firstConstant(TestConverter converter, NumberConstant constant, int start) {
        switch (op) {
            case ADD:
                return convertAdditionConstant(converter, constant, start);
            case MULTIPLY:
                return convertMultiplicationConstant(converter, constant, start);
            case POWER:
                return convertPowConstant(converter, constant, start);
            case BITWISE_AND:
                return convertAndConstant(converter, constant, start);
            case BITWISE_OR:
                return convertOrConstant(converter, constant, start);
            case BITWISE_XOR:
                return convertXorConstant(converter, constant, start);
            default:
                return convertInner(start);
        }
    }

    // FIXME: Run code for side-effects

    @NotNull
    private List<Byte> convertAdditionConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertSubtractionConstant(
            TestConverter converter, @NotNull NumberConstant constant, int start
    ) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertMultiplicationConstant(
            TestConverter converter, @NotNull NumberConstant constant, int start
    ) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(0))));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert(start);
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.U_MINUS.value);
            return bytes;
        } else if (bigValue.signum() > 0 && bigValue.bitCount() == 1) {
            var powerOfTwo = bigValue.bitLength() - 1;
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(powerOfTwo))));
            bytes.add(Bytecode.L_BITSHIFT.value);
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertDivisionConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Division by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, start, "divide");
        } else if (bigValue.equals(BigInteger.ONE)) {
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.decimalConstant())));
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(1))));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.U_MINUS.value);
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.decimalConstant())));
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(1))));
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertFloorDivConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Division by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, start, "divide");
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert(start);
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.U_MINUS.value);
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertModConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Modulo by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, start, "modulo");
        } else if (bigValue.equals(BigInteger.ONE)) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(0))));
            return bytes;
        } else if (bigValue.signum() > 0 && bigValue.bitCount() == 1) {
            var lessOne = bigValue.subtract(BigInteger.ONE);
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(lessOne))));
            bytes.add(Bytecode.BITWISE_AND.value);
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertPowConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(1))));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert(start);
        } else if (bigValue.signum() < 0) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(0))));
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertAndConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(0))));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            return converter.convert(start);
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertOrConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(-1))));
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertXorConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.BITWISE_NOT.value);
            return bytes;
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    private static final BigInteger SHIFT_MAX = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger NEG_SHIFT_MAX = SHIFT_MAX.negate();

    @NotNull
    private List<Byte> convertLShiftConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else if (bigValue.compareTo(SHIFT_MAX) > 0) {
            CompilerWarning.warn(
                    "Shift too big to compute properly, will fail at runtime",
                    WarningType.NO_TYPE, info, lineInfo
            );
            return convertOneConstant(converter, constant, start);
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertRShiftConstant(TestConverter converter, @NotNull NumberConstant constant, int start) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert(start);
        } else if (bigValue.compareTo(NEG_SHIFT_MAX) > 0) {
            CompilerWarning.warn(
                    "Shift magnitude too big to compute properly, will fail at runtime",
                    WarningType.NO_TYPE, info, lineInfo
            );
            return convertOneConstant(converter, constant, start);
        } else {
            return convertOneConstant(converter, constant, start);
        }
    }

    @NotNull
    private List<Byte> convertOneConstant(@NotNull TestConverter converter, NumberConstant constant, int start) {
        assert !op.isUnary();
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(constant)));
        bytes.add(BYTECODE_MAP.get(op).value);
        return bytes;
    }

    @NotNull
    private List<Byte> convertZeroDivision(@NotNull TestConverter converter, int start, String divType) {
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.POP_TOP.value);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.arithmeticErrorConstant())));
        bytes.add(Bytecode.LOAD_CONST.value);
        var message = String.format("Cannot %s by zero", divType);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        return bytes;
    }
}
