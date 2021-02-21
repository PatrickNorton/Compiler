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
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_TRUE.value);
            divergingInfo.possibleContinue();
        } else {
            bytes.add(Bytecode.JUMP.value);
            divergingInfo.knownContinue();
        }
        info.loopManager().addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        return Pair.of(bytes, divergingInfo);
    }
}
