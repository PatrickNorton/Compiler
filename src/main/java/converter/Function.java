package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.List;

public final class Function implements Lined {
    private final LineInfo lineInfo;
    private final FunctionInfo info;
    private final List<Byte> bytes;
    private int max = 0;

    public Function(Lined lineInfo, FunctionInfo info, List<Byte> bytes) {
        this(lineInfo.getLineInfo(), info, bytes);
    }

    public Function(LineInfo lineInfo, FunctionInfo info, List<Byte> bytes) {
        this.lineInfo = lineInfo;
        this.info = info;
        this.bytes = bytes;
    }

    public Function(FunctionInfo info, List<Byte> bytes) {
        this.lineInfo = LineInfo.empty();
        this.info = info;
        this.bytes = bytes;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
