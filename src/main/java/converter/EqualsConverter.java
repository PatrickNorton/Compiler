package main.java.converter;

import main.java.converter.bytecode.ConstantBytecode;
import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class EqualsConverter extends OperatorConverter {
    private final boolean equalsType;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public EqualsConverter(
            boolean equalsType,
            ArgumentNode[] args,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.equalsType = equalsType;
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        var op = equalsType ? OperatorTypeNode.EQUALS : OperatorTypeNode.NOT_EQUALS;
        return defaultConstant(op, info, args);
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.bool()};
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        if (retCount > 1) {
            throw CompilerException.format(
                    "'%s' only returns 1 value, %d expected",
                    lineInfo, equalsType ? "==" : "!=", retCount
            );
        }
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        return switch (args.length) {
            case 0, 1 -> convert0();
            case 2 -> convert2();
            default -> throw CompilerTodoError.of("Cannot compute == for more than 2 operands", lineInfo);
        };
    }

    private BytecodeList convert0() {
        assert args.length == 0 || args.length == 1;
        CompilerWarning.warnf(
                "'%s' with < 2 operands will always be %b",
                WarningType.TRIVIAL_VALUE, info,
                lineInfo, equalsType ? "==" : "!=", equalsType
        );
        // Have to get side-effects
        var conv = sideEffects();
        var bytes = new BytecodeList(conv);
        bytes.add(Bytecode.POP_TOP);
        bytes.add(Bytecode.LOAD_CONST, new ConstantBytecode(LangConstant.of(equalsType), info));
        return bytes;
    }

    @NotNull
    private BytecodeList sideEffects() {
        assert args.length == 0 || args.length == 1;
        return args.length == 0 ? new BytecodeList()
                : TestConverter.bytes(args[0].getArgument(), info, 1);
    }

    @NotNull
    private BytecodeList convert2() {
        assert args.length == 2;
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        // TODO: Equals for types where info doesn't match
        var returnType = converter.returnType()[0];
        boolean useId = returnType instanceof UserType && returnType.operatorInfo(OpSpTypeNode.EQUALS, info).isEmpty();
        BytecodeList bytes = new BytecodeList(converter.convert());
        bytes.addAll(TestConverter.bytes(args[1].getArgument(), info, 1));
        bytes.add(useId ? Bytecode.IDENTICAL : Bytecode.EQUAL);
        if (!equalsType) {
            bytes.add(Bytecode.BOOL_NOT);
        }
        return bytes;
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        throw asException(lineInfo);
    }
}
