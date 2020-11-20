package main.java.converter;

/**
 * The class for keeping track of which byte identifiers are allocated to which
 * type of switch table.
 *
 * @author Patrick Norton
 * @see SwitchTable
 */
public enum TableBytes {
    /**
     * 0: {@link CompactSwitchTable}
     */
    COMPACT,

    /**
     * 1: {@link BigSwitchTable}
     */
    BIG,

    /**
     * 2: {@link StringSwitchTable}
     */
    STRING,

    /**
     * 3: {@link CharSwitchTable}
     */
    CHAR,
    ;

    public byte byteValue() {
        return (byte) ordinal();
    }
}
