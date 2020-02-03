package main.java.converter;

import main.java.parser.NumberNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NumberConverter implements ConstantConverter {
    private CompilerInfo info;
    private NumberNode node;
    private int retCount;

    public NumberConverter(CompilerInfo info, NumberNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            return Collections.emptyList();
        }
        int constIndex = info.addConstant(constant());
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes((short) constIndex));
        return bytes;
    }

    @NotNull
    @Override
    public LangConstant constant() {
        return LangConstant.of(node);
    }

    @Override
    public TypeObject returnType() {
        return constant() instanceof IntConstant ? Builtins.INT : Builtins.DECIMAL;
    }
}
