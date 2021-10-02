package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A bytecode value representing a position on the stack (relative to the top).
 *
 * @author Patrick Norton
 * @see main.java.converter.Bytecode Bytecode
 * @see BytecodeValue
 */
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

