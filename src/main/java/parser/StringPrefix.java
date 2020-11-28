package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The prefixes for strings.
 *
 * @author Patrick Norton
 * @see StringNode
 * @see FormattedStringNode
 */
public enum StringPrefix {
    FORMATTED('f'),
    RAW('r'),
    REGEX('e'),
    BYTES('b'),
    CHAR('c'),
    BYTE('y'),
    ;

    public final char value;

    private static final Map<Character, StringPrefix> values;
    private static final Set<StringPrefix> INVALID_TOGETHER =
            Collections.unmodifiableSet(EnumSet.of(REGEX, BYTES, CHAR, BYTE));
    private static final Set<StringPrefix> INVALID_2 =
            Collections.unmodifiableSet(EnumSet.of(FORMATTED, CHAR, BYTE));

    @Contract(pure = true)
    StringPrefix(char c) {
        this.value = c;
    }

    static {
        Map<Character, StringPrefix> temp = new HashMap<>();
        for (StringPrefix s : StringPrefix.values()) {
            temp.put(s.value, s);
        }
        values = Collections.unmodifiableMap(temp);
    }

    /**
     * Get the prefix from a character
     * @param c The character of the prefix
     * @return The prefix
     */
    public static StringPrefix getPrefix(char c) {
        return values.get(c);
    }

    /**
     * Get the prefixes from a string.
     * @param chars The characters to be parsed
     * @return The string prefixes
     */
    @NotNull
    public static EnumSet<StringPrefix> getPrefixes(@NotNull String chars) {
        EnumSet<StringPrefix> prefixes = EnumSet.noneOf(StringPrefix.class);
        boolean hasUnique = false;
        boolean hasUnique2 = false;
        for (char c : chars.toCharArray()) {
            StringPrefix prefix = getPrefix(c);
            if (prefixes.contains(prefix)) {
                throw new ParserException("Invalid prefix combination " + chars);
            } else if (INVALID_TOGETHER.contains(prefix)) {
                if (hasUnique) {
                    throw new ParserException("Invalid prefix combination " + chars);
                } else {
                    hasUnique = true;
                }
            }
            if (INVALID_2.contains(prefix)) {
                if (hasUnique2) {
                    throw new ParserException("Invalid prefix combination " + chars);
                } else {
                    hasUnique2 = true;
                }
            }
            prefixes.add(prefix);
        }
        return prefixes;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return Character.toString(value);
    }
}
