package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BytecodeValue {
    void writeBytes(@NotNull List<Byte> bytes);

    @NotNull
    String strValue(@NotNull CompilerInfo info);
}
