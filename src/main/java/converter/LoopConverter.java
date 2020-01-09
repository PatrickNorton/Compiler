package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class LoopConverter implements BaseConverter {
    protected final CompilerInfo info;

    public LoopConverter(CompilerInfo info) {
        this.info = info;
    }

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        info.enterLoop();
        var bytes = trueConvert(start);
        info.exitLoop(start, bytes);
        return bytes;
    }

    protected abstract List<Byte> trueConvert(int start);
}
