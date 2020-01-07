package main.java.converter;

import main.java.parser.NumberNode;

import java.util.ArrayList;
import java.util.List;

public final class NumberConverter implements ConstantConverter {
    private CompilerInfo info;
    private NumberNode node;

    public NumberConverter(CompilerInfo info, NumberNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public List<Byte> convert(int start) {
        int constIndex = info.addConstant(constant());
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes((short) constIndex));
        return bytes;
    }

    @Override
    public LangConstant constant() {
        return LangConstant.of(node);
    }

    @Override
    public TypeObject returnType() {
        return Builtins.INT;
    }
}
