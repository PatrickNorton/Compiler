package main.java.converter;

import main.java.parser.TernaryNode;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class TernaryConverter implements TestConverter {
    private final TernaryNode node;
    private final CompilerInfo info;
    private final int retCount;

    public TernaryConverter(CompilerInfo info, TernaryNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        var cond = TestConverter.constantReturn(node.getStatement(), info ,1);
        if (cond.isPresent() && cond.orElseThrow().boolValue().isPresent()) {
            var bool = cond.orElseThrow().boolValue().orElseThrow();
            return TestConverter.constantReturn(bool ? node.getIfTrue() : node.getIfFalse(), info, retCount);
        }
        return Optional.empty();
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var ifTrue = TestConverter.returnType(node.getIfTrue(), info, retCount);
        var ifFalse = TestConverter.returnType(node.getIfFalse(), info, retCount);
        assert retCount <= ifTrue.length && retCount <= ifFalse.length;
        var result = new TypeObject[retCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = TypeObject.union(ifTrue[i], ifFalse[i]);
        }
        return result;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        var condConverter = TestConverter.of(info, node.getStatement(), 1);
        var boolVal = constantBool(condConverter);
        if (boolVal.isPresent()) {
            return convertOpt(boolVal.orElseThrow());
        }
        var bytes = new BytecodeList(condConverter.convert());
        var jump1 = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_FALSE, jump1);
        var ifTrueConverter = TestConverter.of(info, node.getIfTrue(), retCount);
        bytes.addAll(ifTrueConverter.convert());
        var retType = returnType()[0];
        if (retCount == 1 && OptionTypeObject.needsMakeOption(retType, ifTrueConverter.returnType()[0])) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        var jump2 = info.newJumpLabel();
        bytes.add(Bytecode.JUMP, jump2);
        bytes.addLabel(jump1);
        var ifFalseConverter = TestConverter.of(info, node.getIfFalse(), retCount);
        bytes.addAll(ifFalseConverter.convert());
        if (retCount == 1 && OptionTypeObject.needsMakeOption(retType, ifFalseConverter.returnType()[0])) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        bytes.addLabel(jump2);
        return bytes;
    }

    private OptionalBool constantBool(TestConverter condConverter) {
        return condConverter.constantReturn().map(LangConstant::boolValue).orElse(OptionalBool.empty());
    }

    @NotNull
    private BytecodeList convertOpt(boolean condVal) {
        CompilerWarning.warnf(
                "Condition of ternary always evaluates to %b",
                WarningType.UNREACHABLE, info, node.getStatement(), condVal
        );
        var evaluated = condVal ? node.getIfTrue() : node.getIfFalse();
        var notEvaluated = condVal ? node.getIfFalse() : node.getIfTrue();
        TestConverter.bytes(notEvaluated, info, retCount);  // Check for errors, but don't add to bytes
        return TestConverter.bytes(evaluated, info, retCount);
    }
}
