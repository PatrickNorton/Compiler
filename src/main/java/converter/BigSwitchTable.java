package main.java.converter;

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

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) 1);
        bytes.addAll(Util.intToBytes(values.size()));
        for (var val : values.entrySet()) {
            bytes.addAll(BigintConstant.convertBigint(val.getKey()));
            bytes.addAll(Util.intToBytes(val.getValue()));
        }
        bytes.addAll(Util.intToBytes(defaultVal));
        return bytes;
    }
}
