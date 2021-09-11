package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ArgcBytecode implements BytecodeValue {
    private final short argc;

    public ArgcBytecode(short argc) {
        this.argc = argc;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(argc));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
        return Short.toString(argc);
    }
}
