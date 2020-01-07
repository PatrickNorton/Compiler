package main.java.converter;

import main.java.parser.StringNode;

import java.util.ArrayList;
import java.util.List;

public final class StringConverter implements ConstantConverter {
    private CompilerInfo info;
    private StringNode node;

    public StringConverter(CompilerInfo info, StringNode node) {
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
        return Builtins.STR;
    }
}
