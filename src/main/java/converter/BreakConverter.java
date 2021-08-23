package main.java.converter;

import main.java.parser.BreakStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

public final class BreakConverter implements BaseConverter {
    private final BreakStatementNode node;
    private final CompilerInfo info;

    public BreakConverter(CompilerInfo info, BreakStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        return convertAndReturn().getKey();
    }

    @Override
    @NotNull
    public Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        var bytes = new BytecodeList();
        var divergingInfo = new DivergingInfo();
        var levels = node.getLoops();
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
                    bytes.add(Bytecode.JUMP, info.loopManager().breakLabel(levels));
                } else {
                    CompilerWarning.warn(
                            "'break' condition is always false\n" +
                                    "Note: 'break if false' will never be taken and can be removed",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                }
            } else {
                bytes.addAll(condConverter.convert());
                bytes.add(Bytecode.JUMP_TRUE, info.loopManager().breakLabel(levels));
            }
            divergingInfo.possibleBreak(levels);
        } else {
            bytes.add(Bytecode.JUMP, info.loopManager().breakLabel(levels));
            divergingInfo.knownBreak(levels);
        }
        return Pair.of(bytes, divergingInfo);
    }
}
