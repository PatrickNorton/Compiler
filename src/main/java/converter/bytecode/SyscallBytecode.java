package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Syscalls;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The bytecode value representing a specific function number.
 * <p>
 *     For a complete list of syscall indices, see {@link Syscalls}.
 * </p>
 *
 * @author Patrick Norton
 * @see BytecodeValue
 * @see main.java.converter.Bytecode Bytecode
 * @see Syscalls
 */
public final class SyscallBytecode implements BytecodeValue {
    private final short syscall;

    public SyscallBytecode(String name) {
        this((short) Syscalls.get(name));
    }

    public SyscallBytecode(short value) {
        this.syscall = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(syscall));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return String.format("%d (%s)", syscall, Syscalls.nameOf(syscall));
    }
}
