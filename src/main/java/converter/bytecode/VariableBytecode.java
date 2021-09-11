package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VariableBytecode implements BytecodeValue {
    private final short variable;

    public VariableBytecode(short value) {
        this.variable = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(variable));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Short.toString(variable);
    }
}
