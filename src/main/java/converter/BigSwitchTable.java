package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BigSwitchTable implements SwitchTable {
    private final Map<BigInteger, Integer> values;
    private final int defaultVal;

    public BigSwitchTable(Map<BigInteger, Integer> values, int default_stmt) {
        this.values = values;
        this.defaultVal = default_stmt;
    }

    /**
     * Converts the table into a byte representation.
     * <p>
     *     The representation is as follows:
     * <code><pre>
     * [byte] 1 (see {@link TableBytes#BIG})
     * The number of values
     * For each:
     *     [{@link BigintConstant#convertBigint Bigint}]The number
     *     The index to jump to
     * The default place to jump to
     * </pre></code>
     * </p>
     *
     * @see SwitchTable#toBytes()
     * @return The list of bytes represented
     */
    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(TableBytes.BIG.byteValue());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values.entrySet()) {
            bytes.addAll(BigintConstant.convertBigint(val.getKey()));
            bytes.addAll(Util.intToBytes(val.getValue()));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }

    @Override
    @NotNull
    public String strDisassembly() {
        var value = new StringBuilder();
        for (var pair : values.entrySet()) {
            value.append(String.format("%d: %d%n", pair.getKey(), pair.getValue()));
        }
        value.append(String.format("default: %d%n", defaultVal));
        return value.toString();
    }
}
