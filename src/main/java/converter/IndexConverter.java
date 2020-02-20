package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
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

    @Override
    public TypeObject returnType() {
        return TestConverter.returnType(node.getVar(), info, 1).operatorReturnType(OpSpTypeNode.GET_ATTR);
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getVar(), info, 1));
        for (var index : node.getIndices()) {
            bytes.addAll(TestConverter.bytes(start, index, info, 1));
        }
        bytes.add(Bytecode.LOAD_SUBSCRIPT.value);
        bytes.addAll(Util.shortToBytes((short) node.getIndices().length));
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }
}
