package main.java.converter.bytecode;

import main.java.converter.CompilerInfo;
import main.java.converter.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FunctionNoBytecode implements BytecodeValue{
    private final short fnNo;

    public FunctionNoBytecode(short value) {
        this.fnNo = value;
    }

    public short getFnNo() {
        return fnNo;
    }

    @Override
    public void writeBytes(@NotNull List<Byte> bytes) {
        bytes.addAll(Util.shortToBytes(fnNo));
    }

    @Override
    @NotNull
    public String strValue(@NotNull CompilerInfo info) {
         return String.format("%d (%s)", fnNo, info.getFunctions().get(fnNo).getName());
    }
}
