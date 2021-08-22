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
     *     For the currently used bytes, see {@link TableBytes}
     * </p>
     *
     * @return The list of bytes represented
     * @see TableBytes
     */
    List<Byte> toBytes();

    /**
     * The {@link String string} representation of the table, for use in
     * disassembly printing.
     *
     * @return The representation
     */
    String strDisassembly();
}
