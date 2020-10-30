package main.java.parser;

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
    ;

    public final char value;

    private static final Map<Character, StringPrefix> values;
    private static final Set<StringPrefix> INVALID_TOGETHER = Collections.unmodifiableSet(EnumSet.of(REGEX, BYTES));

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

    public static EnumSet<StringPrefix> getPrefixes(String chars) {
        EnumSet<StringPrefix> prefixes = EnumSet.noneOf(StringPrefix.class);
        for (char c : chars.toCharArray()) {
            StringPrefix prefix = getPrefix(c);
            if (prefixes.contains(prefix)) {
                throw new ParserException("Invalid prefix combination " + chars);
            }
            prefixes.add(prefix);
        }
        if (prefixes.containsAll(INVALID_TOGETHER)) {
            throw new ParserException("Invalid prefix combination " + chars);
        }
        return prefixes;
    }

    @Override
    public String toString() {
        return Character.toString(value);
    }
}
