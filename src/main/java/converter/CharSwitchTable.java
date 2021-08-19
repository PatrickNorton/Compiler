package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CharSwitchTable implements SwitchTable {
    private final int functionNo;
    private final Map<Integer, Integer> values;
    private final int defaultVal;

    public CharSwitchTable(int functionNo, Map<Integer, Integer> values, int defaultVal) {
        this.functionNo = functionNo;
        this.values = values;
        this.defaultVal = defaultVal;
    }

    /**
     * Converts the table into a byte representation.
     * <p>
     *     The representation is as follows:
     * <code><pre>
     * [byte] 3 (see {@link TableBytes#CHAR})
     * The number of values
     * For each:
     *     The character
     *     The index to jump to
     * The default place to jump to
     * </pre></code>
     * </p>
     *
     * @see SwitchTable#toBytes
     * @return The list of bytes represented
     */
    @Override
    @NotNull
    public List<Byte> toBytes(Map<Integer, Integer> translation) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.CHAR.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values.entrySet()) {
            bytes.addAll(Util.intToBytes(val.getKey()));
            bytes.addAll(Util.intToBytes(translation.get(val.getValue())));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }

    @Override
    public String strDisassembly() {
        var value = new StringBuilder();
        for (var pair : values.entrySet()) {
            var chr = new CharConstant(pair.getKey()).name();
            value.append(String.format("%s: %d%n", chr, pair.getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal));
        return value.toString();
    }

    @Override
    public int functionNo() {
        return functionNo;
    }
}
