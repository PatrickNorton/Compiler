package main.java.converter;

import main.java.parser.BreakStatementNode;

import java.util.ArrayList;
import java.util.List;

public class BreakConverter implements BaseConverter {
    private BreakStatementNode node;
    private CompilerInfo info;

    public BreakConverter(CompilerInfo info, BreakStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (!node.getCond().isEmpty()) {
            bytes.addAll(BaseConverter.bytes(start, node.getCond(), info));
            bytes.add(Bytecode.JUMP_TRUE.value);
        } else {
            bytes.add(Bytecode.JUMP.value);

        }
        info.addBreak(node.getLoops(), start + bytes.size());
        bytes.addAll(Util.intToBytes(0));
        return bytes;
    }
}
