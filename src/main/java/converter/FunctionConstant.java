package main.java.converter;

import java.util.ArrayList;
import java.util.List;

public class FunctionConstant implements LangConstant {
    private int functionIndex;

    public FunctionConstant(int index) {
        this.functionIndex = index;
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.FUNCTION.ordinal());
        bytes.addAll(Util.intToBytes(functionIndex));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.CALLABLE;
    }
}
