package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.GlobalCompilerInfo;
import main.java.converter.LangConstant;
import main.java.converter.TempConst;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: Remove explicit 'short' so that constants can be optimized

/**
 * A bytecode value representing a constant.
 *
 * @author Patrick Norton
 * @see main.java.converter.Bytecode Bytecode
 * @see BytecodeValue
 */
public final class ConstantBytecode implements BytecodeValue{
    private final LangConstant constant;
    private final short value;

    public ConstantBytecode(LangConstant constant, @NotNull CompilerInfo info) {
        this(constant, info.constIndex(constant));
    }

    public ConstantBytecode(LangConstant constant, @NotNull GlobalCompilerInfo info) {
        this(constant, info.constIndex(constant));
    }

    public ConstantBytecode(LangConstant constant, short value) {
        this.constant = constant;
        this.value = value;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(value));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
        if (constant instanceof TempConst) {
            // FIXME: Remove this special case
            assert constant.getType().equals(info.getConstant(value).getType());
            return String.format("%d (%s)", value, info.getConstant(value).name(info.getConstants()));
        } else {
            assert constant.equals(info.getConstant(value));
            return String.format("%d (%s)", value, constant.name(info.getConstants()));
        }
    }
}
