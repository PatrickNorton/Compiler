package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface BytecodeValue
        permits ArgcBytecode, ConstantBytecode, FunctionNoBytecode, LocationBytecode, OperatorBytecode,
        StackPosBytecode, SyscallBytecode, TableNoBytecode, VariableBytecode, VariantBytecode {
    void writeBytes(@NotNull List<Byte> bytes);

    @NotNull
    String strValue(@NotNull CompilerInfo info);
}
