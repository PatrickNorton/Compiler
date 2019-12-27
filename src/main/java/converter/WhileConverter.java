package main.java.converter;

import main.java.parser.WhileStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WhileConverter extends LoopConverter {
    private WhileStatementNode node;

    public WhileConverter(CompilerInfo info, WhileStatementNode node) {
        super(info);
        this.node = node;
    }

    @Override
    protected void trueConvert(int start, @NotNull List<Byte> bytes) {
        var body = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP.size(), node.getBody(), info);
        int jumpTarget = start + bytes.size() + Bytecode.JUMP.size() + body.size();
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(intToBytes(jumpTarget));
        bytes.addAll(body);
        var cond = BaseConverter.bytes(start + bytes.size(), node.getCond(), info);
        bytes.addAll(cond);
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(intToBytes(start));
        if (!node.getNobreak().isEmpty()) {
            var nobreak = BaseConverter.bytes(start + bytes.size(), node.getNobreak(), info);
            int eol = start + bytes.size() + nobreak.size() + Bytecode.JUMP.size();
            bytes.add(Bytecode.JUMP.value);
            bytes.addAll(intToBytes(eol));
            bytes.addAll(nobreak);
        }
    }
}
