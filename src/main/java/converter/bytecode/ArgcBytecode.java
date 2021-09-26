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

    private static final ArgcBytecode ZERO = new ArgcBytecode((short) 0);
    private static final ArgcBytecode ONE = new ArgcBytecode((short) 1);

    @NotNull
    public static ArgcBytecode zero() {
        return ZERO;
    }

    @NotNull
    public static ArgcBytecode one() {
        return ONE;
    }
}
