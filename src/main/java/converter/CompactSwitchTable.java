package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CompactSwitchTable implements SwitchTable {
    private final List<Integer> values;
    private final int defaultVal;

    public CompactSwitchTable(List<Integer> values, int defaultVal) {
        this.values = values;
        this.defaultVal = defaultVal;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) 0);
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values) {
            bytes.addAll(Util.intToBytes(val));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }

    @Override
    @NotNull
    public String strDisassembly() {
        var value = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            value.append(String.format("%d: %d%n", i, values.get(i)));
        }
        value.append(String.format("default: %d%n", defaultVal));
        return value.toString();
    }
}
