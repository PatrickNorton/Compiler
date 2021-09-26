package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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
    public BytecodeList convert() {
        switch (op) {
            case NULL_COERCE:
                return convertNullCoerce();
            case NOT_NULL:
                return convertNotNull();
            case OPTIONAL:
                return convertQuestion();
            default:
                throw CompilerInternalError.of("", lineInfo);
        }
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        if (op == OperatorTypeNode.OPTIONAL) {
            return convertQuestionAs();
        } else {
            throw asException(lineInfo);
        }
    }

    @NotNull
    private BytecodeList convertNullCoerce() {
        assert op == OperatorTypeNode.NULL_COERCE;
        var firstConverter = TestConverter.of(info, args[0].getArgument(), 1);
        if (!(firstConverter.returnType()[0] instanceof OptionTypeObject)) {  // Non-optional return types won't be null
            CompilerWarning.warn(
                    "Using ?? operator on non-optional value", WarningType.TRIVIAL_VALUE, info, args[0]
            );
            return firstConverter.convert();
        } else if (firstConverter.returnType()[0].equals(Builtins.nullType())) {
            CompilerWarning.warn(
                    "Using ?? operator on value that is always null", WarningType.TRIVIAL_VALUE, info, args[0]
            );
            return TestConverter.bytes(args[1].getArgument(), info, 1);
        }
        var bytes = unwrapSecond(firstConverter);
        var postLabel = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_NN, postLabel);
        addPostJump(bytes, postLabel);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    @NotNull
    private BytecodeList convertNotNull() {
        assert op == OperatorTypeNode.NOT_NULL;
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        BytecodeList bytes = new BytecodeList(converter.convert());
        var retType = converter.returnType()[0];
        if (retType.equals(Builtins.nullType())) {
            throw CompilerException.of(
                    "Cannot use !! operator on variable on variable with type null",
                    args[0]
            );
        } else if (retType instanceof OptionTypeObject) {
            bytes.addAll(unwrapOption(info, args[0].toString()));
        } else {
            CompilerWarning.warn("Used !! operator on non-optional value",
                    WarningType.TRIVIAL_VALUE, info, args[0].getLineInfo());
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    @NotNull
    private BytecodeList convertQuestion() {
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = converter.returnType()[0];
        if (!(retType instanceof OptionTypeObject)) {
            throw CompilerException.format("Cannot use ? on a non-optional type '%s", args[0], retType.name());
        }
        var bytes = new BytecodeList(converter.convert());
        bytes.add(Bytecode.IS_SOME);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    private void addPostJump(@NotNull BytecodeList bytes, Label postLabel) {
        bytes.add(Bytecode.POP_TOP);
        bytes.addAll(TestConverter.bytes(args[1].getArgument(), info, 1));
        bytes.addLabel(postLabel);
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
    private Pair<BytecodeList, TypeObject> convertQuestionAs() {
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = converter.returnType()[0];
        if (!(retType instanceof OptionTypeObject)) {
            throw CompilerException.format("Cannot use ? on a non-optional type '%s'", args[0], retType.name());
        }
        var resultType = ((OptionTypeObject) retType).getOptionVal();
        var bytes = unwrapSecond(converter);
        bytes.add(Bytecode.IS_SOME);
        return Pair.of(bytes, resultType);
    }

    @NotNull
    public static BytecodeList unwrapOption(CompilerInfo info, String value) {
        var bytes = new BytecodeList();
        bytes.add(Bytecode.DUP_TOP);
        var jump = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_NN, jump);
        bytes.add(Bytecode.POP_TOP);
        bytes.loadConstant(Builtins.nullErrorConstant(), info);
        var message = String.format("Value %s asserted non-null, was null", value);
        bytes.loadConstant(LangConstant.of(message), info);
        bytes.add(Bytecode.THROW_QUICK, ArgcBytecode.one());
        bytes.addLabel(jump);
        bytes.add(Bytecode.UNWRAP_OPTION);
        return bytes;
    }

    @NotNull
    private static BytecodeList unwrapSecond(@NotNull TestConverter converter) {
        var bytes = new BytecodeList(converter.convert());
        bytes.add(Bytecode.DUP_TOP);
        bytes.add(Bytecode.UNWRAP_OPTION);
        bytes.add(Bytecode.SWAP_2);
        return bytes;
    }
}
