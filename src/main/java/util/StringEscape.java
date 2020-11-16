package main.java.util;

public final class StringEscape {
    private StringEscape() {}

    public static String unescape(String value) {
        var sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            sb.append(escaped(value.charAt(i)));
        }
        return sb.toString();
    }

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
                return Character.toString(c);
        }
    }
}
