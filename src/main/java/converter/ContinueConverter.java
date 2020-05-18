package main.java.converter;

import main.java.parser.ContinueStatementNode;
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
        List<Byte> bytes = new ArrayList<>();
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_TRUE.value);
        } else {
            bytes.add(Bytecode.JUMP.value);
        }
        info.addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        return bytes;
    }
}
