import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum StringPrefix {
    FORMATTED('f'),
    RAW('r'),
    REGEX('e'),
    BYTES('b'),
    ;

    public final char value;
    private static final Map<Character, StringPrefix> values;

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

    public static StringPrefix getPrefix(char c) {
        return values.get(c);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return Character.toString(value);
    }
}
