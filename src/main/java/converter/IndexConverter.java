package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IndexConverter implements TestConverter {
    private final IndexNode node;
    private final CompilerInfo info;
    private final int retCount;

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
            return new TypeObject[]{Builtins.TYPE.generify(type)};
        }
        var operator = node.getIndices()[0] instanceof SliceNode ? OpSpTypeNode.GET_SLICE : OpSpTypeNode.GET_ATTR;
        return TestConverter.returnType(node.getVar(), info, 1)[0].operatorReturnType(operator, info);
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getVar(), info, 1));
        if (node.getIndices()[0] instanceof SliceNode) {
            checkSliceType();
            assert node.getIndices().length == 1;
            bytes.addAll(new SliceConverter(info, (SliceNode) node.getIndices()[0]).convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.GET_SLICE.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 1));
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

    private void checkSliceType() {
        var retType = TestConverter.returnType(node.getVar(), info, 1)[0];
        var fnInfo = retType.tryOperatorInfo(node.getLineInfo(), OpSpTypeNode.GET_SLICE, info);
        if (!fnInfo.matches(new Argument("", Builtins.SLICE))) {
            throw CompilerException.format(
                    "Type '%s' has an operator [:] that does not take a slice as its argument",
                    node, retType.name()
            );
        }
    }
}
