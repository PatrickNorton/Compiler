package main.java.converter;

import main.java.parser.ContinueStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

public final class ContinueConverter implements BaseConverter {
    private final ContinueStatementNode node;
    private final CompilerInfo info;

    public ContinueConverter(CompilerInfo info, ContinueStatementNode node) {
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
        if (!node.getCond().isEmpty()) {
            var condConverter = TestConverter.of(info, node.getCond(), 1);
            var constant = condConverter.constantReturn();
            if (constant.isPresent() && constant.orElseThrow().boolValue().isPresent()) {
                var value = constant.orElseThrow().boolValue().orElseThrow();
                if (value) {
                    CompilerWarning.warn(
                            "'continue' condition is always true\n" +
                                    "Note: 'continue if true' is equivalent to 'continue'",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                    bytes.add(Bytecode.JUMP, info.loopManager().continueLabel());
                } else {
                    CompilerWarning.warn(
                            "'continue' condition is always false\n" +
                                    "Note: 'continue if false' will never be taken and can be removed",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                }
            } else {
                bytes.addAll(condConverter.convert());
                bytes.add(Bytecode.JUMP_TRUE, info.loopManager().continueLabel());
            }
            divergingInfo.possibleContinue();
        } else {
            bytes.add(Bytecode.JUMP, info.loopManager().continueLabel());
            divergingInfo.knownContinue();
        }
        return Pair.of(bytes, divergingInfo);
    }
}
