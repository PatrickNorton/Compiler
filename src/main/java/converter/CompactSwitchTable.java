package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CompactSwitchTable implements SwitchTable {
    private final List<Label> values;
    private final Label defaultVal;

    public CompactSwitchTable(List<Label> values, Label defaultVal) {
        this.values = values;
        this.defaultVal = defaultVal;
    }

    /**
     * Converts the table into a byte representation.
     * <p>
     *     The representation is as follows:
     * <code><pre>
     * [byte] 0 (see {@link TableBytes#COMPACT})
     * The number of values
     * The max value
     * For each i < max:
     *     i
     *     The index to jump to
     * The default index to jump to
     * </pre></code>
     * </p>
     * @see SwitchTable#toBytes
     * @return The list of bytes represented
     */
    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.COMPACT.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values) {
            bytes.addAll(Util.intToBytes(val.getValue()));
        }
        bytes.addAll(Util.intToBytes(defaultVal.getValue()));
        return bytes;
    }

    @Override
    @NotNull
    public String strDisassembly() {
        var value = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            value.append(String.format("%d: %d%n", i, values.get(i).getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal.getValue()));
        return value.toString();
    }

}
