package main.java.converter;

import main.java.parser.TernaryNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TernaryConverter implements TestConverter {
    private final TernaryNode node;
    private final CompilerInfo info;
    private final int retCount;

    public TernaryConverter(CompilerInfo info, TernaryNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var ifTrue = TestConverter.of(info, node.getIfTrue(), retCount);
        var ifFalse = TestConverter.of(info, node.getIfFalse(), retCount);
        return new TypeObject[] {TypeObject.union(ifTrue.returnType()[0], ifFalse.returnType()[0])};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getStatement(), info, 1));
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jump1 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getIfTrue(), info, retCount));
        bytes.add(Bytecode.JUMP.value);
        int jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump1);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getIfFalse(), info, 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump2);
        return bytes;
    }
}
