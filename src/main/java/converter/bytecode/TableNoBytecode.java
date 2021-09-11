package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TableNoBytecode implements BytecodeValue {
    private final short table;

    public TableNoBytecode(short value) {
        this.table = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(table));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Short.toString(table);
    }
}
