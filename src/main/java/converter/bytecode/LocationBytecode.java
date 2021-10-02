package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Label;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The bytecode value representing a jump location.
 * <p>
 *     This bytecode doesn't store an integer value, but instead uses a {@link
 *     Label directly}. This allows refactoring to take place without needing
 *     to rewrite every bytecode value.
 * </p>
 *
 * @author Patrick Norton
 * @see main.java.converter.Bytecode Bytecode
 * @see BytecodeValue
 */
public final class LocationBytecode implements BytecodeValue {
    private final Label value;

    public LocationBytecode(Label value) {
        this.value = value;
    }

    public Label getLabel() {
        return value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.intToBytes(value.getValue()));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return Integer.toString(value.getValue());
    }
}
