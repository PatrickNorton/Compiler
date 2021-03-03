package main.java.converter;

import main.java.parser.DoStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DoWhileConverter extends LoopConverter {
    private final DoStatementNode node;

    public DoWhileConverter(CompilerInfo info, DoStatementNode node) {
        super(info);
        this.node = node;
    }

    @NotNull
    @Override
    protected List<Byte> trueConvert(int start) {
        List<Byte> bytes = new ArrayList<>(convertBody(start));
        info.loopManager().setContinuePoint(start + bytes.size());
        bytes.addAll(convertCond(start + bytes.size()));
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.intToBytes(start));
        return bytes;
    }

    private List<Byte> convertCond(int start) {
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
        return converter.convert(start);
    }

    private List<Byte> convertBody(int start) {
        var pair = BaseConverter.bytesWithReturn(start, node.getBody(), info);
        var bytes = pair.getKey();
        var divergingInfo = pair.getValue();
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes exactly once", WarningType.UNREACHABLE, info, node);
        }
        return bytes;
    }
}
