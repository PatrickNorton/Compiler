package main.java.converter;

import main.java.parser.ContinueStatementNode;

import java.util.ArrayList;
import java.util.List;

public final class ContinueConverter implements BaseConverter {
    private ContinueStatementNode node;
    private CompilerInfo info;

    public ContinueConverter(CompilerInfo info, ContinueStatementNode node) {
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
        info.addContinue(start + bytes.size());
        bytes.addAll(Util.intToBytes(0));
        return bytes;
    }
}
