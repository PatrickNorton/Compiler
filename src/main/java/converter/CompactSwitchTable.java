package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CompactSwitchTable implements SwitchTable {
    private final int functionNo;
    private final List<Integer> values;
    private final int defaultVal;

    public CompactSwitchTable(int functionNo, List<Integer> values, int defaultVal) {
        this.functionNo = functionNo;
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
     * @see SwitchTable#toBytes)
     * @return The list of bytes represented
     */
    @Override
    @NotNull
    public List<Byte> toBytes(Map<Integer, Integer> translation) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.COMPACT.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values) {
            bytes.addAll(Util.intToBytes(translation.get(val)));
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

    @Override
    public int functionNo() {
        return functionNo;
    }
}
