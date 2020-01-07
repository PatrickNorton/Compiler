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
        String name = node.getName();
        boolean isConst = info.variableIsConstant(node.getName());
        var bytecode = isConst ? Bytecode.LOAD_CONST : Bytecode.LOAD_VALUE;
        List<Byte> bytes = new ArrayList<>(bytecode.size());
        bytes.add(bytecode.value);
        short index = (short) (isConst ? info.constIndex(name) : info.varIndex(name));
        bytes.addAll(Util.shortToBytes(index));
        return bytes;
    }
}
