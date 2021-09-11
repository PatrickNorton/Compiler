package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OperatorBytecode implements BytecodeValue {
    private final short operator;

    public OperatorBytecode(short value) {
        this.operator = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(operator));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return String.format("%d (%s)", operator, OpSpTypeNode.values()[operator]);
    }
}
