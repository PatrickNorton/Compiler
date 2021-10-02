package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The bytecode representing an operator ordinal.
 *
 * @author Patrick Norton
 * @see main.java.converter.Bytecode Bytecode
 * @see BytecodeValue
 */
public final class OperatorBytecode implements BytecodeValue {
    private final OpSpTypeNode operator;

    public OperatorBytecode(@NotNull OpSpTypeNode value) {
        this.operator = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes((short) operator.ordinal()));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return String.format("%d (%s)", operator.ordinal(), operator);
    }
}
