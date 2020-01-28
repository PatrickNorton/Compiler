package main.java.converter;

import main.java.parser.TernaryNode;

import java.util.ArrayList;
import java.util.List;

public class TernaryConverter implements TestConverter {
    private TernaryNode node;
    private CompilerInfo info;

    public TernaryConverter(CompilerInfo info, TernaryNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public TypeObject returnType() {
        var ifTrue = TestConverter.of(info, node.getIfTrue());
        var ifFalse = TestConverter.of(info, node.getIfFalse());
        return TypeObject.union(ifTrue.returnType(), ifFalse.returnType());
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(BaseConverter.bytes(start, node.getStatement(), info));
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jump1 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getIfTrue(), info));
        bytes.add(Bytecode.JUMP.value);
        int jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump1);
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getIfFalse(), info));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump2);
        return bytes;
    }
}
