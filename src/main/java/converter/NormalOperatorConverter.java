package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
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
    public BytecodeList convert() {
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        if (op == OperatorTypeNode.NOT_EQUALS) {
            return convertNotEquals();
        }
        var opt = optimizeConstant();
        if (opt.isPresent()) {
            return opt.orElseThrow();
        }
        return convertInner();
    }

    @NotNull
    private BytecodeList convertInner() {
        assert op != OperatorTypeNode.NOT_EQUALS;
        var bytes = new BytecodeList();
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
            bytes.addAll(TestConverter.bytes(arg.getArgument(), info, 1));
        }
        var bytecode = BYTECODE_MAP.get(op);
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(bytecode);
        } else if (MANDATORY_ARG_COUNT.contains(op)) {
            throw CompilerException.format(
                    "Cannot call operator '%s' with %d operands (expected exactly %d)",
                    lineInfo, op, opCount, op.isUnary() ? 1 : 2
            );
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    @NotNull
    private BytecodeList convertNotEquals() {
        int opCount = args.length;
        assert opCount == 2 && op == OperatorTypeNode.NOT_EQUALS;
        var bytes = new BytecodeList(TestConverter.bytes(args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(args[1].getArgument(), info, 1));
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(Bytecode.EQUAL);
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        bytes.add(Bytecode.BOOL_NOT);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    private Optional<BytecodeList> optimizeConstant() {
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
            return Optional.of(secondConstant(firstConverter, (NumberConstant) secondConstant.orElseThrow()));
        }
        var firstConstant = firstConverter.constantReturn();
        if (firstConstant.isPresent()) {
            return Optional.of(firstConstant(secondConverter, (NumberConstant) firstConstant.orElseThrow()));
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        throw asException(lineInfo);
    }

    private BytecodeList secondConstant(TestConverter converter, NumberConstant constant) {
        switch (op) {
            case ADD:
                return convertAdditionConstant(converter, constant);
            case SUBTRACT:
                return convertSubtractionConstant(converter, constant);
            case MULTIPLY:
                return convertMultiplicationConstant(converter, constant);
            case DIVIDE:
                return convertDivisionConstant(converter, constant);
            case FLOOR_DIV:
                return convertFloorDivConstant(converter, constant);
            case POWER:
                return convertPowConstant(converter, constant);
            case LEFT_BITSHIFT:
                return convertLShiftConstant(converter, constant);
            case RIGHT_BITSHIFT:
                return convertRShiftConstant(converter, constant);
            case BITWISE_AND:
                return convertAndConstant(converter, constant);
            case BITWISE_OR:
                return convertOrConstant(converter, constant);
            case BITWISE_XOR:
                return convertXorConstant(converter, constant);
            case MODULO:
                return convertModConstant(converter, constant);
            default:
                return convertOneConstant(converter, constant);
        }
    }

    private BytecodeList firstConstant(TestConverter converter, NumberConstant constant) {
        switch (op) {
            case ADD:
                return convertAdditionConstant(converter, constant);
            case MULTIPLY:
                return convertMultiplicationConstant(converter, constant);
            case POWER:
                return convertPowConstant(converter, constant);
            case BITWISE_AND:
                return convertAndConstant(converter, constant);
            case BITWISE_OR:
                return convertOrConstant(converter, constant);
            case BITWISE_XOR:
                return convertXorConstant(converter, constant);
            default:
                return convertInner();
        }
    }

    // FIXME: Run code for side-effects

    @NotNull
    private BytecodeList convertAdditionConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertSubtractionConstant(
            TestConverter converter, @NotNull NumberConstant constant
    ) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertMultiplicationConstant(
            TestConverter converter, @NotNull NumberConstant constant
    ) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(0)));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert();
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.U_MINUS);
            return bytes;
        } else if (bigValue.signum() > 0 && bigValue.bitCount() == 1) {
            var powerOfTwo = bigValue.bitLength() - 1;
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(powerOfTwo)));
            bytes.add(Bytecode.L_BITSHIFT);
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertDivisionConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Division by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, "divide");
        } else if (bigValue.equals(BigInteger.ONE)) {
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.decimalConstant()));
            bytes.add(Bytecode.CALL_TOS, info.constIndex(LangConstant.of(1)));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.U_MINUS);
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.decimalConstant()));
            bytes.add(Bytecode.CALL_TOS, info.constIndex(LangConstant.of(1)));
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertFloorDivConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Division by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, "divide");
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert();
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.U_MINUS);
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertModConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            CompilerWarning.warn(
                    "Modulo by the constant 0 will always result in an error",
                    WarningType.ZERO_DIVISION, info, LineInfo.empty()
            );
            return convertZeroDivision(converter, "modulo");
        } else if (bigValue.equals(BigInteger.ONE)) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(0)));
            return bytes;
        } else if (bigValue.signum() > 0 && bigValue.bitCount() == 1) {
            var lessOne = bigValue.subtract(BigInteger.ONE);
            BytecodeList bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(lessOne)));
            bytes.add(Bytecode.BITWISE_AND);
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertPowConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(1)));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE)) {
            return converter.convert();
        } else if (bigValue.signum() < 0) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(0)));
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertAndConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(0)));
            return bytes;
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            return converter.convert();
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertOrConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            BytecodeList bytes = new BytecodeList();
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(-1)));
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertXorConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else if (bigValue.equals(BigInteger.ONE.negate())) {
            var bytes = new BytecodeList();
            bytes.add(Bytecode.BITWISE_NOT);
            return bytes;
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    private static final BigInteger SHIFT_MAX = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger NEG_SHIFT_MAX = SHIFT_MAX.negate();

    @NotNull
    private BytecodeList convertLShiftConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else if (bigValue.compareTo(SHIFT_MAX) > 0) {
            CompilerWarning.warn(
                    "Shift too big to compute properly, will fail at runtime",
                    WarningType.NO_TYPE, info, lineInfo
            );
            return convertOneConstant(converter, constant);
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertRShiftConstant(TestConverter converter, @NotNull NumberConstant constant) {
        var bigValue = constant.bigValue();
        if (bigValue.equals(BigInteger.ZERO)) {
            return converter.convert();
        } else if (bigValue.compareTo(NEG_SHIFT_MAX) > 0) {
            CompilerWarning.warn(
                    "Shift magnitude too big to compute properly, will fail at runtime",
                    WarningType.NO_TYPE, info, lineInfo
            );
            return convertOneConstant(converter, constant);
        } else {
            return convertOneConstant(converter, constant);
        }
    }

    @NotNull
    private BytecodeList convertOneConstant(@NotNull TestConverter converter, NumberConstant constant) {
        assert !op.isUnary();
        var bytes = new BytecodeList(converter.convert());
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(constant));
        bytes.add(BYTECODE_MAP.get(op));
        return bytes;
    }

    @NotNull
    private BytecodeList convertZeroDivision(@NotNull TestConverter converter, String divType) {
        var bytes = new BytecodeList(converter.convert());
        bytes.add(Bytecode.POP_TOP);
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.arithmeticErrorConstant()));
        var message = String.format("Cannot %s by zero", divType);
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(message)));
        bytes.add(Bytecode.THROW_QUICK, 1);
        return bytes;
    }
}
