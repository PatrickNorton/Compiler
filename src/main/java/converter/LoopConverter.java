package main.java.converter;

import java.util.List;

public abstract class LoopConverter implements BaseConverter {
    protected final CompilerInfo info;
    private final boolean hasContinue;

    public LoopConverter(CompilerInfo info) {
        this(info, true);
    }

    public LoopConverter(CompilerInfo info, boolean hasContinue) {
        this.info = info;
        this.hasContinue = hasContinue;
    }

    @Override
    public final List<Byte> convert(int start) {
        info.loopManager().enterLoop(hasContinue);
        info.addStackFrame();
        var bytes = trueConvert(start);
        info.loopManager().exitLoop(start, bytes);
        info.removeStackFrame();
        return bytes;
    }

    protected abstract List<Byte> trueConvert(int start);
}
