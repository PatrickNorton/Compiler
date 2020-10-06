package main.java.converter;

import main.java.parser.TernaryNode;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TernaryConverter implements TestConverter {
    private final TernaryNode node;
    private final CompilerInfo info;
    private final int retCount;

    public TernaryConverter(CompilerInfo info, TernaryNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var ifTrue = TestConverter.of(info, node.getIfTrue(), retCount);
        var ifFalse = TestConverter.of(info, node.getIfFalse(), retCount);
        return new TypeObject[] {TypeObject.union(ifTrue.returnType()[0], ifFalse.returnType()[0])};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        var condConverter = TestConverter.of(info, node.getStatement(), 1);
        var boolVal = constantBool(condConverter);
        if (boolVal.isPresent()) {
            return convertOpt(start, boolVal.orElseThrow());
        }
        List<Byte> bytes = new ArrayList<>(condConverter.convert(start));
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jump1 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        var ifTrueConverter = TestConverter.of(info, node.getIfTrue(), retCount);
        bytes.addAll(ifTrueConverter.convert(start + bytes.size()));
        var retType = returnType()[0];
        if (retCount == 1 && OptionTypeObject.needsMakeOption(retType, ifTrueConverter.returnType()[0])) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        bytes.add(Bytecode.JUMP.value);
        int jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump1);
        var ifFalseConverter = TestConverter.of(info, node.getIfFalse(), retCount);
        bytes.addAll(ifFalseConverter.convert(start + bytes.size()));
        if (retCount == 1 && OptionTypeObject.needsMakeOption(retType, ifFalseConverter.returnType()[0])) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump2);
        return bytes;
    }

    private OptionalBool constantBool(TestConverter condConverter) {
        return condConverter.constantReturn().map(LangConstant::boolValue).orElse(OptionalBool.empty());
    }

    private List<Byte> convertOpt(int start, boolean condVal) {
        CompilerWarning.warnf("Condition of ternary always evaluates to %b", node.getStatement(), condVal);
        var evaluated = condVal ? node.getIfTrue() : node.getIfFalse();
        var notEvaluated = condVal ? node.getIfFalse() : node.getIfTrue();
        TestConverter.bytes(start, notEvaluated, info, retCount);  // Check for errors, but don't add to bytes
        return TestConverter.bytes(start, evaluated, info, retCount);
    }
}
