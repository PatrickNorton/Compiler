package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IndexConverter implements TestConverter {
    private IndexNode node;
    private CompilerInfo info;
    private int retCount;

    public IndexConverter(CompilerInfo info, IndexNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var type = TypeObject.of(info, node);
        if (type != null) {
            return new TypeObject[]{new GenerifiedTypeTypeObject(type)};
        }
        var operator = node.getIndices()[0] instanceof SliceNode ? OpSpTypeNode.GET_SLICE : OpSpTypeNode.GET_ATTR;
        return TestConverter.returnType(node.getVar(), info, 1)[0].operatorReturnType(operator);
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getVar(), info, 1));
        if (node.getIndices()[0] instanceof SliceNode) {
            assert node.getIndices().length == 1;
            var slice = (SliceNode) node.getIndices()[0];
            bytes.addAll(TestConverter.bytes(start + bytes.size(), slice.getStart(), info, 1));
            bytes.addAll(TestConverter.bytes(start + bytes.size(), slice.getEnd(), info, 1));
            bytes.addAll(TestConverter.bytes(start + bytes.size(), slice.getStep(), info, 1));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.GET_SLICE.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 3));
        } else {
            for (var index : node.getIndices()) {
                bytes.addAll(TestConverter.bytes(start, index, info, 1));
            }
            bytes.add(Bytecode.LOAD_SUBSCRIPT.value);
            bytes.addAll(Util.shortToBytes((short) node.getIndices().length));
            if (retCount == 0) {
                bytes.add(Bytecode.POP_TOP.value);
            }
        }
        return bytes;
    }
}
