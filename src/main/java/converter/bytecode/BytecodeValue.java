package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The interface representing the possible value types bytecode can have.
 *
 * @author Patrick Norton
 */
public sealed interface BytecodeValue
        permits ArgcBytecode, ConstantBytecode, FunctionNoBytecode, LocationBytecode, OperatorBytecode,
        StackPosBytecode, SyscallBytecode, TableNoBytecode, VariableBytecode, VariantBytecode {
    /**
     * Writes this bytecode to a list of bytes.
     *
     * @param bytes The byte list to write to
     */
    void writeBytes(@NotNull List<Byte> bytes);

    /**
     * Format this bytecode as a string.
     *
     * @param info The {@link CompilerInfo} to get information from
     * @return The formatted string
     */
    @NotNull
    String strValue(@NotNull CompilerInfo info);
}
