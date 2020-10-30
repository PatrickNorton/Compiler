package main.java.converter;

import main.java.parser.DoStatementNode;

import java.util.ArrayList;
import java.util.List;

public final class DoWhileConverter extends LoopConverter {
    private final DoStatementNode node;

    public DoWhileConverter(CompilerInfo info, DoStatementNode node) {
        super(info);
        this.node = node;
    }

    @Override
    protected List<Byte> trueConvert(int start) {
        var body = BaseConverter.bytes(start, node.getBody(), info);
        List<Byte> bytes = new ArrayList<>(body);
        info.loopManager().setContinuePoint(start + bytes.size());
        var cond = TestConverter.bytes(start + bytes.size(), node.getConditional(), info, 1);
        bytes.addAll(cond);
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.intToBytes(start));
        return bytes;
    }
}
