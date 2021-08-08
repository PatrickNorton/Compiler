package main.java.converter;

import main.java.parser.BreakStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BreakConverter implements BaseConverter {
    private final BreakStatementNode node;
    private final CompilerInfo info;

    public BreakConverter(CompilerInfo info, BreakStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        return convertAndReturn(start).getKey();
    }

    @Override
    @NotNull
    public Pair<List<Byte>, DivergingInfo> convertAndReturn(int start) {
        List<Byte> bytes = new ArrayList<>();
        var divergingInfo = new DivergingInfo();
        boolean mayJump;
        if (!node.getCond().isEmpty()) {
            var condConverter = TestConverter.of(info, node.getCond(), 1);
            var constant = condConverter.constantReturn();
            if (constant.isPresent() && constant.orElseThrow().boolValue().isPresent()) {
                var value = constant.orElseThrow().boolValue().orElseThrow();
                if (value) {
                    CompilerWarning.warn(
                            "'break' condition is always true\n" +
                                    "Note: 'break if true' is equivalent to 'continue'",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                    bytes.add(Bytecode.JUMP.value);
                    mayJump = true;
                } else {
                    CompilerWarning.warn(
                            "'break' condition is always false\n" +
                                    "Note: 'break if false' will never be taken and can be removed",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                    mayJump = false;
                }
            } else {
                bytes.addAll(condConverter.convert(start));
                bytes.add(Bytecode.JUMP_TRUE.value);
                mayJump = true;
            }
            divergingInfo.possibleBreak(node.getLoops());
        } else {
            bytes.add(Bytecode.JUMP.value);
            divergingInfo.knownBreak(node.getLoops());
            mayJump = true;
        }
        if (mayJump) {
            info.loopManager().addBreak(node.getLoops(), start + bytes.size());
        }
        bytes.addAll(Util.zeroToBytes());
        return Pair.of(bytes, divergingInfo);
    }
}
