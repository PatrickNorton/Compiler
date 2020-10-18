package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
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
        return allInts(info, args).flatMap(values -> IntArithmetic.computeConst(op, values));
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.BOOL};
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        if (retCount > 1) {
            throw CompilerException.format(
                    "'%s' only returns 1 value, %d expected",
                    lineInfo, equalsType ? "==" : "!=", retCount
            );
        }
        switch (args.length) {
            case 0:
            case 1:
                return convert0(start);
            case 2:
                return convert2(start);
            default:
                throw CompilerTodoError.of("Cannot compute == for more than 2 operands", lineInfo);
        }
    }

    private List<Byte> convert0(int start) {
        assert args.length == 0 || args.length == 1;
        CompilerWarning.warnf(
                "'%s' with < 2 operands will always be %b",
                lineInfo, equalsType ? "==" : "!=", equalsType
        );
        // Have to get side-effects
        var conv = TestConverter.bytes(start, args[0].getArgument(), info, 1);
        List<Byte> bytes = new ArrayList<>(conv);
        bytes.add(Bytecode.POP_TOP.value);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(equalsType))));
        return bytes;
    }

    private List<Byte> convert2(int start) {
        assert args.length == 2;
        var converter = TestConverter.of(info, args[0].getArgument(), 1);
        // TODO: Equals for types where info doesn't match
        boolean useId = converter.returnType()[0].operatorInfo(OpSpTypeNode.EQUALS, info).isEmpty();
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.addAll(TestConverter.bytes(start, args[1].getArgument(), info, 1));
        bytes.add(useId ? Bytecode.IDENTICAL.value : Bytecode.EQUAL.value);
        if (!equalsType) {
            bytes.add(Bytecode.BOOL_NOT.value);
        }
        return bytes;
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        throw asException(lineInfo);
    }
}
