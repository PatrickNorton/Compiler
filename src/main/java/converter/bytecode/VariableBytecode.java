package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The bytecode value representing a variable index.
 *
 * @author Patrick Norton
 * @see BytecodeValue
 * @see main.java.converter.Bytecode Bytecode
 */
public final class VariableBytecode implements BytecodeValue {
    private final short variable;

    public VariableBytecode(short value) {
        this.variable = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(variable));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Short.toString(variable);
    }
}
