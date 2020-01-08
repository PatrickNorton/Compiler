package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class LoopConverter implements BaseConverter {
    protected final CompilerInfo info;

    public LoopConverter(CompilerInfo info) {
        this.info = info;
    }

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        info.enterLoop();
        trueConvert(start, bytes);
        info.exitLoop(start, bytes);
        return bytes;
    }

    protected abstract void trueConvert(int start, List<Byte> bytes);
}
