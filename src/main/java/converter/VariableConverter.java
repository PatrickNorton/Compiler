package main.java.converter;

import main.java.parser.VariableNode;

import java.util.ArrayList;
import java.util.List;

public class VariableConverter implements TestConverter {
    private CompilerInfo info;
    private VariableNode node;

    public VariableConverter(CompilerInfo info, VariableNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public TypeObject returnType() {
        return info.getType(node.getName());
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(Bytecode.LOAD_VALUE.size());
        bytes.add(Bytecode.LOAD_VALUE.value);
        bytes.addAll(Util.shortToBytes((short) info.varIndex(node.getName())));
        return bytes;
    }
}
