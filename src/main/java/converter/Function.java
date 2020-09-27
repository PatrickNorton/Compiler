package main.java.converter;

import java.util.List;

public final class Function {
    private final FunctionInfo info;
    private final List<Byte> bytes;

    public Function(FunctionInfo info, List<Byte> bytes) {
        this.info = info;
        this.bytes = bytes;
    }

    public String getName() {
        return info.getName();
    }

    public boolean matches(Argument... args) {
        return info.matches(args);
    }

    public TypeObject[] getReturns() {
        return info.getReturns();
    }

    public List<Byte> getBytes() {
        return bytes;
    }

    public FunctionInfo getInfo() {
        return info;
    }

    public boolean isGenerator() {
        return info.isGenerator();
    }
}
