package main.java.converter;

import main.java.parser.IncrementNode;

import java.util.ArrayList;
import java.util.List;

public class IncrementConverter implements BaseConverter {
    private IncrementNode node;
    private CompilerInfo info;

    public IncrementConverter(CompilerInfo info, IncrementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        var converter = TestConverter.of(info, node.getVariable());
        if (!converter.returnType().isSubclass(Builtins.INT)) {
            throw CompilerException.format(
                    "TypeError: Object of type %s cannot be incremented",
                    node.getLineInfo(), converter.returnType());
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.LOAD_CONST.value);
        int constIndex = info.addConstant(LangConstant.of(1));
        bytes.addAll(Util.shortToBytes((short) constIndex));
        bytes.add(Bytecode.PLUS.value);
        return bytes;
    }
}
