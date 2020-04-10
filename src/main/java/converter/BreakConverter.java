package main.java.converter;

import main.java.parser.BreakStatementNode;
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
        List<Byte> bytes = new ArrayList<>();
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_TRUE.value);
        } else {
            bytes.add(Bytecode.JUMP.value);
        }
        info.addBreak(node.getLoops(), start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        return bytes;
    }
}
