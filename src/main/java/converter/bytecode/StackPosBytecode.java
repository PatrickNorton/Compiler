package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class StackPosBytecode implements BytecodeValue {
    private final short position;

    public StackPosBytecode(short value) {
        this.position = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(position));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Short.toString(position);
    }

    @Contract(value = " -> new", pure = true)
    @NotNull
    public static StackPosBytecode zero() {
        return new StackPosBytecode((short) 0);
    }
}

