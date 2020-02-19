package main.java.converter;

import main.java.parser.LiteralNode;

import java.util.ArrayList;
import java.util.List;

public class LiteralConverter implements TestConverter {  // FIXME: Generics
    private LiteralNode node;
    private CompilerInfo info;
    private int retCount;

    public LiteralConverter(CompilerInfo info, LiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public TypeObject returnType() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), value, info));
            }
        } else {
            assert retCount == 1;
            boolean isList = node.getBraceType().equals("[");
            for (var value : node.getBuilders()) {
                bytes.addAll(TestConverter.bytes(start + bytes.size(), value, info, 1));
            }
            bytes.add((isList ? Bytecode.LIST_CREATE : Bytecode.SET_CREATE).value);
            bytes.addAll(Util.shortToBytes((short) node.getBuilders().length));
        }
        return bytes;
    }
}
