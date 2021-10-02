package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The bytecode value representing an enum variant.
 *
 * @author Patrick Norton
 * @see BytecodeValue
 * @see main.java.converter.Bytecode Bytecode
 */
public final class VariantBytecode  implements BytecodeValue {
    private final short variant;

    public VariantBytecode(short value) {
        this.variant = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(variant));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Short.toString(variant);
    }
}
