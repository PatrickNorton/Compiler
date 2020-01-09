package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class LoopConverter implements BaseConverter {
    protected final CompilerInfo info;
    private boolean hasContinue;

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
        info.enterLoop(hasContinue);
        var bytes = trueConvert(start);
        info.exitLoop(start, bytes);
        return bytes;
    }

    protected abstract List<Byte> trueConvert(int start);
}
