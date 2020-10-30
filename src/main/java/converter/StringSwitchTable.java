package main.java.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StringSwitchTable implements SwitchTable {
    private final Map<String, Integer> values;
    private final int defaultVal;

    public StringSwitchTable(Map<String, Integer> values, int defaultVal) {
        this.values = values;
        this.defaultVal = defaultVal;
    }

    /**
     * Converts the table into a byte representation.
     * <p>
     *     The representation is as follows:
     * <code><pre>
     * [byte] 2 (see {@link TableBytes#STRING})
     * The number of values
     * For each:
     *     The string value
     *     The index to jump to
     * The default place to jump to
     * </pre></code>
     * </p>
     *
     * @see SwitchTable#toBytes()
     * @return The list of bytes represented
     */
    @Override

    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.STRING.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var pair : values.entrySet()) {
            bytes.addAll(StringConstant.strBytes(pair.getKey()));
            bytes.addAll(Util.intToBytes(pair.getValue()));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }

    @Override

    public String strDisassembly() {
        var value = new StringBuilder();
        for (var pair : values.entrySet()) {
            value.append(String.format("\"%s\": %d%n", pair.getKey(), pair.getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal));
        return value.toString();
    }
}
