package main.java.util;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Methods related to escaping strings.
 *
 * @author Patrick Norton
 */
public final class StringEscape {
    private StringEscape() {}

    /**
     * Adds escape codes to the given String.
     *
     * @param value The value to replace with escape codes
     * @return The unescaped value
     */
    public static String unescape(String value) {
        return value.codePoints().mapToObj(StringEscape::escaped).collect(Collectors.joining());
    }

    /**
     * Gets the escaped version of a codepoint.
     * <p>
     *     This will return the original character if it is printable, e.g. its
     *     Unicode class is one of the ones in {@link #PRINTABLE_CLASSES}.
     *     Otherwise, it will use one of the escape codes this language allows.
     * </p>
     *
     * @param i A Unicode codepoint
     * @return The escaped character
     */
    public static String escaped(int i) {
        if (Character.isBmpCodePoint(i)) {
            return escaped((char) i);
        } else {
            if (PRINTABLE_CLASSES.contains((byte) Character.getType(i))) {
                return new String(Character.toChars(i));
            } else {
                return String.format("\\U%08X", i);
            }
        }
    }

    /**
     * Gets the escaped version of a character.
     * <p>
     *     This will return the original character if it is printable, e.g. its
     *     Unicode class is one of the ones in {@link #PRINTABLE_CLASSES}.
     *     Otherwise, it will use one of the escape codes this language allows.
     * </p>
     *
     * @param c A character
     * @return The escaped character
     */
    public static String escaped(char c) {
        switch (c) {
            case '\\':
                return "\\\\";
            case '"':
                return "\\\"";
            case '\0':
                return "\\0";
            case '\7':
                return "\\a";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case '\013':
                return "\\v";
            default:
                if (PRINTABLE_CLASSES.contains((byte) Character.getType(c))) {
                    return Character.toString(c);
                } else if (c < 0x100) {
                    return String.format("\\x%02X", (int) c);
                } else {
                    return String.format("\\u%04X", (int) c);
                }
        }
    }

    private static final Set<Byte> PRINTABLE_CLASSES = Set.of(
            Character.UPPERCASE_LETTER,
            Character.LOWERCASE_LETTER,
            Character.TITLECASE_LETTER,
            Character.MODIFIER_LETTER,
            Character.OTHER_LETTER,
            Character.NON_SPACING_MARK,
            Character.ENCLOSING_MARK,
            Character.DECIMAL_DIGIT_NUMBER,
            Character.LETTER_NUMBER,
            Character.OTHER_NUMBER,
            Character.SPACE_SEPARATOR,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.CONNECTOR_PUNCTUATION,
            Character.OTHER_PUNCTUATION,
            Character.MATH_SYMBOL,
            Character.CURRENCY_SYMBOL,
            Character.MODIFIER_SYMBOL,
            Character.OTHER_SYMBOL,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION
    );
}
