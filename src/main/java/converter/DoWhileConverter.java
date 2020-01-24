package main.java.converter;

import main.java.parser.DoStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DoWhileConverter extends LoopConverter {
    private DoStatementNode node;

    public DoWhileConverter(CompilerInfo info, DoStatementNode node) {
        super(info);
        this.node = node;
    }

    @NotNull
    @Override
    protected List<Byte> trueConvert(int start) {
        var body = BaseConverter.bytes(start, node.getBody(), info);
        List<Byte> bytes = new ArrayList<>(body);
        info.setContinuePoint(start + bytes.size());
        var cond = BaseConverter.bytes(start + bytes.size(), node.getConditional(), info);
        bytes.addAll(cond);
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.intToBytes(start));
        return bytes;
    }
}
