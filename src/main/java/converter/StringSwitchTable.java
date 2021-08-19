package main.java.converter;

import main.java.util.StringEscape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StringSwitchTable implements SwitchTable {
    private final int functionNo;
    private final Map<String, Integer> values;
    private final int defaultVal;

    public StringSwitchTable(int functionNo, Map<String, Integer> values, int defaultVal) {
        this.functionNo = functionNo;
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
     * @see SwitchTable#toBytes
     * @return The list of bytes represented
     */
    @Override
    @NotNull
    public List<Byte> toBytes(Map<Integer, Integer> translation) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.STRING.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var pair : values.entrySet()) {
            bytes.addAll(StringConstant.strBytes(pair.getKey()));
            bytes.addAll(Util.intToBytes(translation.get(pair.getValue())));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }

    @Override
    @NotNull
    public String strDisassembly() {
        var value = new StringBuilder();
        for (var pair : values.entrySet()) {
            value.append(String.format("\"%s\": %d%n", StringEscape.escape(pair.getKey()), pair.getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal));
        return value.toString();
    }

    @Override
    public int functionNo() {
        return functionNo;
    }
}
