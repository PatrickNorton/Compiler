package main.java.converter;

import main.java.parser.NumberNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NumberConverter implements ConstantConverter {
    private final CompilerInfo info;
    private final NumberNode node;
    private final int retCount;

    public NumberConverter(CompilerInfo info, NumberNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Numeric literal unused", WarningType.UNUSED, info, node);
            return Collections.emptyList();
        } else if (retCount > 1) {
            throw CompilerException.format("Numeric literals return 1 value, %d were expected", node, retCount);
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

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var constant = constant();
        return new TypeObject[] {
                constant instanceof IntConstant || constant instanceof BigintConstant ? Builtins.INT : Builtins.DECIMAL
        };
    }
}
