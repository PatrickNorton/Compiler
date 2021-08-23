package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public final class InConverter extends OperatorConverter {
    private final boolean inType;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public InConverter(
            boolean inType,
            ArgumentNode[] args,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.inType = inType;
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        if (args.length != 2) {
            return Optional.empty();
        }
        var arg0const = TestConverter.constantReturn(args[0].getArgument(), info, 1);
        var arg1const = TestConverter.constantReturn(args[1].getArgument(), info, 1);
        if (arg0const.isEmpty() || arg1const.isEmpty()) {
            return Optional.empty();
        }
        var arg0 = arg0const.orElseThrow();
        var arg1 = arg1const.orElseThrow();
        if (arg1 instanceof BytesConstant) {
            return bytesConst(arg0, (BytesConstant) arg1);
        } else if (arg1 instanceof RangeConstant) {
            return rangeConst(arg0, (RangeConstant) arg1);
        } else if (arg1 instanceof StringConstant) {
            return stringConst(arg0, (StringConstant) arg1);
        } else {
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.bool()};
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        if (args.length != 2) {
            throw CompilerException.format("Expected 2 arguments for 'in' operator, got %d", lineInfo, args.length);
        }
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        var containedConverter = TestConverter.of(info, args[0].getArgument(), 1);
        var containerConverter = TestConverter.of(info, args[1].getArgument(), 1);
        var opInfo = containerConverter.returnType()[0].tryOperatorInfo(lineInfo, OpSpTypeNode.IN, info);
        if (!opInfo.matches(new Argument("", containedConverter.returnType()[0]))) {
            var args = opInfo.getArgs().getNormalArgs();
            var argTypes = String.join(", ", TypeObject.name(Argument.typesOf(args)));
            throw CompilerException.format(
                    "Cannot call operator 'in' on type '%s':%n" +
                            "Arguments received: %s%nArguments expected: %s",
                    lineInfo, containerConverter.returnType()[0].name(), argTypes
            );
        }
        var bytes = new BytecodeList(containedConverter.convert());
        bytes.addAll(containerConverter.convert());
        bytes.add(Bytecode.SWAP_2);
        bytes.add(Bytecode.CONTAINS);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        } else if (!inType) {
            bytes.add(Bytecode.BOOL_NOT);
        }
        return bytes;
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        throw asException(lineInfo);
    }

    private Optional<LangConstant> bytesConst(LangConstant arg0, BytesConstant arg1) {
        byte value;
        if (arg0 instanceof IntConstant) {
            var val = ((IntConstant) arg0).getValue();
            if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE) {
                return Optional.of(Builtins.FALSE);
            }
            value = (byte) val;
        } else if (arg0 instanceof BigintConstant) {
            var val = ((BigintConstant) arg0).getValue();
            if (!Util.fitsInByte(val)) {
                return Optional.of(Builtins.FALSE);
            }
            value = val.byteValueExact();
        } else {
            return Optional.empty();
        }
        var bytes = arg1.getValue();
        return Optional.of(LangConstant.of(bytes.contains(value)));
    }

    private Optional<LangConstant> rangeConst(LangConstant arg0, RangeConstant arg1) {
        var maybeVal = IntArithmetic.convertConst(arg0);
        if (maybeVal.isEmpty()) {
            return Optional.empty();
        }
        BigInteger value = maybeVal.orElseThrow();
        var mStart = arg1.getStart();
        var mStop = arg1.getStop();
        var step = arg1.getStep().orElse(BigInteger.ONE);
        if (mStart.isPresent()) {
            var start = mStart.orElseThrow();
            if (mStop.isPresent()) {
                var stop = mStop.orElseThrow();
                boolean isBetween;
                if (step.signum() >= 0) {
                    isBetween = value.compareTo(start) >= 0
                            && value.compareTo(stop) < 0
                            && goesInto(value, start, step);
                } else {
                    isBetween = value.compareTo(stop) >= 0
                            && value.compareTo(start) < 0
                            && goesInto(value, start, step.negate());
                }
                return Optional.of(LangConstant.of(isBetween));
            } else {
                boolean isBetween;
                if (step.signum() >= 0) {
                    isBetween = value.compareTo(start) >= 0 && goesInto(value, start, step);
                } else {
                    isBetween = value.compareTo(start) <= 0 && goesInto(value, start, step.negate());
                }
                return Optional.of(LangConstant.of(isBetween));
            }
        } else {
            if (mStop.isPresent()) {
                var stop = mStop.orElseThrow();
                boolean isBetween;
                if (step.signum() >= 0) {
                    isBetween = value.compareTo(stop) < 0 && goesInto(value, stop, step);
                } else {
                    isBetween = value.compareTo(stop) > 0 && goesInto(value, stop, step.negate());
                }
                return Optional.of(LangConstant.of(isBetween));
            } else {
                if (!step.equals(BigInteger.ONE)) {
                    return Optional.empty();
                } else {
                    return Optional.of(LangConstant.of(true));
                }
            }
        }
    }

    private Optional<LangConstant> stringConst(LangConstant arg0, StringConstant arg1) {
        if (arg0 instanceof CharConstant) {
            var chr = ((CharConstant) arg0).getValue();
            var charSeq = new String(Character.toChars(chr));
            return Optional.of(LangConstant.of(arg1.getValue().contains(charSeq)));
        } else {
            return Optional.empty();
        }
    }

    private static boolean goesInto(BigInteger value, BigInteger start, BigInteger step) {
        // Equivalent to (value - start) % step == 0
        return value.subtract(start).mod(step).signum() == 0;
    }
}
