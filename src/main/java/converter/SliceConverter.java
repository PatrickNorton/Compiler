package main.java.converter;

import main.java.parser.SliceNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SliceConverter implements TestConverter {
    private final SliceNode node;
    private final CompilerInfo info;

    public SliceConverter(CompilerInfo info, SliceNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[0];
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (!node.getStart().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node, info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);
        if (!node.getEnd().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node, info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);
        if (!node.getStep().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node, info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);

        return bytes;
    }
}
