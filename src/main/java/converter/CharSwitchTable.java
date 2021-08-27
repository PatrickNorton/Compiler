package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CharSwitchTable implements SwitchTable {
    private final Map<Integer, Label> values;
    private final Label defaultVal;

    public CharSwitchTable(Map<Integer, Label> values, Label defaultVal) {
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
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.CHAR.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values.entrySet()) {
            bytes.addAll(Util.intToBytes(val.getKey()));
            bytes.addAll(Util.intToBytes(val.getValue().getValue()));
        }
        bytes.addAll(Util.intToBytes(defaultVal.getValue()));
        return bytes;
    }

    @Override
    public String strDisassembly() {
        var value = new StringBuilder();
        for (var pair : values.entrySet()) {
            var chr = new CharConstant(pair.getKey()).name();
            value.append(String.format("%s: %d%n", chr, pair.getValue().getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal.getValue()));
        return value.toString();
    }

}
