package main.java.converter;

import java.util.List;

public interface SwitchTable {
    /**
     * Converts the table into a byte representation.
     * <p>
     *     The representation of a switch table is a {@code byte} representing
     *     the type of switch table, followed by code defined by each table.
     * </p>
     * <p>
     *     The currently used bytes are as follows:
     *     <ol start="0">
     *         <li>{@link CompactSwitchTable#toBytes() CompactSwitchTable}</li>
     *         <li>{@link BigSwitchTable#toBytes() BigSwitchTable}</li>
     *         <li>{@link StringSwitchTable#toBytes() StringSwitchTable}</li>
     *     </ol>
     * </p>
     *
     * @return The list of bytes represented
     */
    List<Byte> toBytes();
    String strDisassembly();
}
