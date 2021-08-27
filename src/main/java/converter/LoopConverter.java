package main.java.converter;

import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    @Override
    public final BytecodeList convert() {
        info.loopManager().enterLoop(info, hasContinue);
        info.addStackFrame();
        var bytes = trueConvert();
        info.loopManager().exitLoop(bytes);
        info.removeStackFrame();
        return bytes;
    }

    @Override
    @NotNull
    public Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        info.loopManager().enterLoop(info, hasContinue);
        info.addStackFrame();
        var pair = trueConvertWithReturn();
        info.loopManager().exitLoop(pair.getKey());
        info.removeStackFrame();
        return Pair.of(pair.getKey(), pair.getValue().removeLevel());
    }

    protected abstract BytecodeList trueConvert();

    protected Pair<BytecodeList, DivergingInfo> trueConvertWithReturn() {
        return Pair.of(trueConvert(), new DivergingInfo());
    }
}
