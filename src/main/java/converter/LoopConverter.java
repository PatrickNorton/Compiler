package main.java.converter;

import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        info.loopManager().enterLoop(hasContinue);
        info.addStackFrame();
        var bytes = trueConvert(start);
        info.loopManager().exitLoop(start, bytes);
        info.removeStackFrame();
        return bytes;
    }

    @Override
    @NotNull
    public Pair<List<Byte>, Boolean> convertAndReturn(int start) {
        info.loopManager().enterLoop(hasContinue);
        info.addStackFrame();
        var pair = trueConvertWithReturn(start);
        info.loopManager().exitLoop(start, pair.getKey());
        info.removeStackFrame();
        return pair;
    }

    protected abstract List<Byte> trueConvert(int start);

    protected Pair<List<Byte>, Boolean> trueConvertWithReturn(int start) {
        return Pair.of(trueConvert(start), false);
    }
}
