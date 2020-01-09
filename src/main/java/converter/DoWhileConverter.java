package main.java.converter;

import main.java.parser.DoStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DoWhileConverter extends LoopConverter {
    private DoStatementNode node;

    public DoWhileConverter(CompilerInfo info, DoStatementNode node) {
        super(info);
        this.node = node;
    }

    @Override
    protected void trueConvert(int start, @NotNull List<Byte> bytes) {
        assert bytes.size() == 0;
        var body = BaseConverter.bytes(start, node.getBody(), info);
        bytes.addAll(body);
        info.setContinuePoint(start + bytes.size());
        var cond = BaseConverter.bytes(start + bytes.size(), node.getConditional(), info);
        bytes.addAll(cond);
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.zeroToBytes());
    }
}
