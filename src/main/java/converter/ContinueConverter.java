package main.java.converter;

import main.java.parser.ContinueStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ContinueConverter implements BaseConverter {
    private final ContinueStatementNode node;
    private final CompilerInfo info;

    public ContinueConverter(CompilerInfo info, ContinueStatementNode node) {
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
                            "'continue' condition is always true\n" +
                                    "Note: 'continue if true' is equivalent to 'continue'",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                    bytes.add(Bytecode.JUMP.value);
                    mayJump = true;
                } else {
                    CompilerWarning.warn(
                            "'continue' condition is always false\n" +
                                    "Note: 'continue if false' will never be taken and can be removed",
                            WarningType.TRIVIAL_VALUE, info, node
                    );
                    mayJump = false;
                }
            } else {
                bytes.addAll(condConverter.convert(start));
                bytes.add(Bytecode.JUMP_TRUE.value);
                mayJump = true;
            }
            divergingInfo.possibleContinue();
        } else {
            bytes.add(Bytecode.JUMP.value);
            divergingInfo.knownContinue();
            mayJump = true;
        }
        if (mayJump) {
            info.loopManager().addContinue(start + bytes.size());
        }
        bytes.addAll(Util.zeroToBytes());
        return Pair.of(bytes, divergingInfo);
    }
}
