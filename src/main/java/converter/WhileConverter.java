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
        assert bytes.size() == 0;
        // While loop starts by jumping to condition, use the fact that a
        // continue statement does the same
        bytes.add(Bytecode.JUMP.value);
        info.addContinue(start + 1);
        bytes.addAll(Util.intToBytes(0));
        var body = BaseConverter.bytes(start + bytes.size(), node.getBody(), info);
        bytes.addAll(body);
        info.setContinuePoint(start + bytes.size());
        var cond = BaseConverter.bytes(start + bytes.size(), node.getCond(), info);
        bytes.addAll(cond);
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.zeroToBytes());
        if (!node.getNobreak().isEmpty()) {
            var nobreak = BaseConverter.bytes(start + bytes.size(), node.getNobreak(), info);
            bytes.addAll(nobreak);
        }
    }
}
