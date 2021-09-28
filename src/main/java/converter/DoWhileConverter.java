package main.java.converter;

import main.java.parser.DoStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

public final class DoWhileConverter extends LoopConverter {
    private final DoStatementNode node;

    public DoWhileConverter(CompilerInfo info, DoStatementNode node) {
        super(info);
        this.node = node;
    }

    @NotNull
    @Override
    protected BytecodeList trueConvert() {
        return trueConvertWithReturn().getKey();
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, DivergingInfo> trueConvertWithReturn() {
        var label = info.newJumpLabel();
        var pair = convertBody();
        var bytes = pair.getKey();
        bytes.addLabel(info.loopManager().continueLabel());
        bytes.addAll(convertCond());
        bytes.add(Bytecode.JUMP_TRUE, label);
        return Pair.of(bytes, pair.getValue());
    }

    @NotNull
    private BytecodeList convertCond() {
        var converter = TestConverter.of(info, node.getConditional(), 1);
        var constantReturn = converter.constantReturn();
        if (constantReturn.isPresent()) {
            var constantBool = constantReturn.orElseThrow().boolValue();
            if (constantBool.isTrue()) {
                CompilerWarning.warn(
                        "'do-while' loop with always-true parameter is equivalent to a 'while true' loop",
                        WarningType.TRIVIAL_VALUE, info, node.getConditional()
                );
            } else if (constantBool.isFalse()) {
                CompilerWarning.warn(
                        "'do-while' loop with always-false parameter will execute exactly once",
                        WarningType.TRIVIAL_VALUE, info, node.getConditional()
                );
            }
        }
        return converter.convert();
    }

    @NotNull
    private Pair<BytecodeList, DivergingInfo> convertBody() {
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
        var bytes = pair.getKey();
        var divergingInfo = pair.getValue();
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes exactly once", WarningType.UNREACHABLE, info, node);
        }
        return Pair.of(bytes, divergingInfo);
    }
}
