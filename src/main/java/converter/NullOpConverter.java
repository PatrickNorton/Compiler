package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class NullOpConverter extends OperatorConverter {
    private final OperatorTypeNode op;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public NullOpConverter(
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
    @NotNull
    public TypeObject[] returnType() {
        switch (op) {
            case NULL_COERCE:
                return nullCoerceReturn();
            case NOT_NULL:
                return notNullReturn();
            case OPTIONAL:
                return new TypeObject[] {Builtins.bool()};
            default:
                throw CompilerInternalError.of("", lineInfo);
        }
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        switch (op) {
            case NULL_COERCE:
                return convertNullCoerce(start);
            case NOT_NULL:
                return convertNotNull(start);
            case OPTIONAL:
                return convertQuestion(start);
            default:
                throw CompilerInternalError.of("", lineInfo);
        }
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        if (op == OperatorTypeNode.OPTIONAL) {
            return convertQuestionAs(start);
        } else {
            throw asException(lineInfo);
        }
    }

    @NotNull
    private List<Byte> convertNullCoerce(int start) {
        assert op == OperatorTypeNode.NULL_COERCE;
        var firstConverter = TestConverter.of(info, args[0].getArgument(), 1);
        if (!(firstConverter.returnType()[0] instanceof OptionTypeObject)) {  // Non-optional return types won't be null
            CompilerWarning.warn(
                    "Using ?? operator on non-optional value", WarningType.TRIVIAL_VALUE, info, args[0]
            );
            return firstConverter.convert(start);
        } else if (firstConverter.returnType()[0].equals(Builtins.nullType())) {
            CompilerWarning.warn(
                    "Using ?? operator on value that is always null", WarningType.TRIVIAL_VALUE, info, args[0]
            );
            return TestConverter.bytes(start, args[1].getArgument(), info, 1);
        }
        List<Byte> bytes = unwrapSecond(start, firstConverter);
        bytes.add(Bytecode.JUMP_NN.value);
        addPostJump(start, bytes);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertNotNull(int start) {
        assert op == OperatorTypeNode.NOT_NULL;
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        var retType = converter.returnType()[0];
        if (retType.equals(Builtins.nullType())) {
            throw CompilerException.of(
                    "Cannot use !! operator on variable on variable with type null",
                    args[0]
            );
        } else if (retType instanceof OptionTypeObject) {
            bytes.addAll(unwrapOption(info, args[0].toString(), start + bytes.size()));
        } else {
            CompilerWarning.warn("Used !! operator on non-optional value",
                    WarningType.TRIVIAL_VALUE, info, args[0].getLineInfo());
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertQuestion(int start) {
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = converter.returnType()[0];
        if (!(retType instanceof OptionTypeObject)) {
            throw CompilerException.format("Cannot use ? on a non-optional type '%s", args[0], retType.name());
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.IS_SOME.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private void addPostJump(int start, @NotNull List<Byte> bytes) {
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.POP_TOP.value);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), args[1].getArgument(), info, 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
    }

    @NotNull
    private TypeObject[] notNullReturn() {
        var retType = TestConverter.returnType(args[0].getArgument(), info, 1)[0];
        if (retType.equals(Builtins.nullType())) {
             // Doesn't particularly matter what, it'll fail later
            return new TypeObject[] {Builtins.throwsType()};
        } else {
            return new TypeObject[] {retType.stripNull()};
        }
    }

    @NotNull
    private TypeObject[] nullCoerceReturn() {
        var ret0 = TestConverter.returnType(args[0].getArgument(), info, 1)[0];
        var ret1 = TestConverter.returnType(args[1].getArgument(), info, 1)[0];
        var result = ret0.equals(Builtins.nullType()) ? ret1 : TypeObject.union(ret0.stripNull(), ret1);
        return new TypeObject[] {result};
    }

    @NotNull
    private Pair<List<Byte>, TypeObject> convertQuestionAs(int start) {
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = converter.returnType()[0];
        if (!(retType instanceof OptionTypeObject)) {
            throw CompilerException.format("Cannot use ? on a non-optional type '%s'", args[0], retType.name());
        }
        var resultType = ((OptionTypeObject) retType).getOptionVal();
        List<Byte> bytes = unwrapSecond(start, converter);
        bytes.add(Bytecode.IS_SOME.value);
        return Pair.of(bytes, resultType);
    }

    @NotNull
    public static List<Byte> unwrapOption(CompilerInfo info, String value, int start) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.JUMP_NN.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.POP_TOP.value);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.nullErrorConstant())));
        bytes.add(Bytecode.LOAD_CONST.value);
        var message = String.format("Value %s asserted non-null, was null", value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        bytes.add(Bytecode.UNWRAP_OPTION.value);
        return bytes;
    }

    @NotNull
    private static List<Byte> unwrapSecond(int start, @NotNull TestConverter converter) {
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.UNWRAP_OPTION.value);
        bytes.add(Bytecode.SWAP_2.value);
        return bytes;
    }
}
