package main.java.converter;

import main.java.parser.SliceNode;
import main.java.parser.TestNode;
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
            checkTypes(node.getStart());
            bytes.addAll(TestConverter.bytes(start, node.getStart(), info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);
        if (!node.getEnd().isEmpty()) {
            checkTypes(node.getEnd());
            bytes.addAll(TestConverter.bytes(start, node.getEnd(), info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);
        if (!node.getStep().isEmpty()) {
            checkTypes(node.getStep());
            bytes.addAll(TestConverter.bytes(start, node.getStep(), info, 1));
        } else {
            bytes.add(Bytecode.LOAD_NULL.value);
        }
        bytes.add(Bytecode.MAKE_OPTION.value);

        return bytes;
    }

    private void checkTypes(TestNode subNode) {
        var retType = TestConverter.returnType(subNode, info, 1)[0];
        if (!Builtins.INT.isSuperclass(retType)) {
            throw CompilerException.format(
                    "Type '%s' is not a superclass of int, cannot be used in a slice",
                    node, retType.name()
            );
        }
    }
}
