package main.java.converter;

import main.java.parser.DoStatementNode;
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
        var label = info.newJumpLabel();
        var bytes = new BytecodeList(convertBody());
        bytes.addLabel(info.loopManager().continueLabel());
        bytes.addAll(convertCond());
        bytes.add(Bytecode.JUMP_TRUE, label);
        return bytes;
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

    private BytecodeList convertBody() {
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
        var bytes = pair.getKey();
        var divergingInfo = pair.getValue();
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes exactly once", WarningType.UNREACHABLE, info, node);
        }
        return bytes;
    }
}
