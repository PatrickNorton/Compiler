package main.java.converter;

import main.java.parser.ReturnStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ReturnConverter implements BaseConverter {
    private ReturnStatementNode node;
    private CompilerInfo info;

    public ReturnConverter(CompilerInfo info, ReturnStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            int jumpTarget = start + bytes.size() + Bytecode.JUMP_FALSE.size() + Bytecode.RETURN.size();
            bytes.add(Bytecode.JUMP_FALSE.value);
            bytes.addAll(Util.intToBytes(jumpTarget));
        }
        bytes.add(Bytecode.RETURN.value);
        bytes.addAll(Util.shortToBytes((short) node.getReturned().size()));
        return bytes;
    }
}
