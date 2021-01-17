package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.List;

public final class Method implements Lined {
    private final LineInfo lineInfo;
    private final MethodInfo info;
    private final List<Byte> bytes;

    public Method(Lined lineInfo, MethodInfo info, List<Byte> bytes) {
        this(lineInfo.getLineInfo(), info, bytes);
    }

    public Method(LineInfo lineInfo, MethodInfo info, List<Byte> bytes) {
        this.lineInfo = lineInfo;
        this.info = info;
        this.bytes = bytes;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getName() {
        return info.getInfo().getName();
    }

    public boolean matches(Argument... args) {
        return info.getInfo().matches(args);
    }

    public TypeObject[] getReturns() {
        return info.getReturns();
    }

    public List<Byte> getBytes() {
        return bytes;
    }

    public MethodInfo getInfo() {
        return info;
    }

    public boolean isGenerator() {
        return info.getInfo().isGenerator();
    }
}
