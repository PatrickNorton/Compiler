package main.java.parser;

import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The class representing a formatted string.
 * <p>
 *     This is separate intentionally from the normal {@link StringNode},
 *     because it has a list of tests which need to be performed in order to be
 *     turned into a proper string.
 * </p>
 * @author Patrick Norton
 * @see StringNode
 * @see StringLikeNode
 */
public class FormattedStringNode extends StringLikeNode {
    private String[] strings;
    private TestNode[] tests;
    private FormatInfo[] formats;

    /**
     * Construct a new FormattedStringNode.
     * @param strings The intermittent string literals
     * @param tests The non-string-literals which are interpolated
     */
    @Contract(pure = true)
    public FormattedStringNode(LineInfo info, String[] strings, TestNode[] tests,
                               FormatInfo[] formats, @NotNull Set<StringPrefix> flags) {
        super(info, flags);
        this.strings = strings;
        this.tests = tests;
        this.formats = formats;
    }

    @Override
    public String[] getStrings() {
        return strings;
    }

    public TestNode[] getTests() {
        return tests;
    }

    public FormatInfo[] getFormats() {
        return formats;
    }

    /**
     * Parse a FormattedStringNode from a {@link String} representing its
     * contents.
     *
     * @param token The token for the contents of the string
     * @return The freshly parsed FormattedStringNode
     */
    @NotNull
    @Contract("_ -> new")
    static FormattedStringNode parse(@NotNull Token token) {
        LineInfo info = token.lineInfo;
        String inside = getContents(token);
        Set<StringPrefix> prefixes = getPrefixes(token);
        assert prefixes.contains(StringPrefix.FORMATTED);
        boolean isRaw = prefixes.contains(StringPrefix.RAW);
        List<String> strings = new ArrayList<>();
        List<TestNode> tests = new ArrayList<>();
        List<FormatInfo> formats = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int newEnd = 0;
        for (int newStart = inside.indexOf('{'); newStart != -1; newStart = inside.indexOf('{', newEnd)) {
            if (isEscaped(inside, newStart)) {
                current.append(inside, newEnd, newStart + 1);
                newEnd = newStart + 1;
                continue;
            }
            strings.add(maybeProcessEscapes(isRaw, current.append(inside, newEnd, newStart).toString(), info));
            current.setLength(0);  // Don't create new instances, help the GC a little
            newEnd = sizeOfBrace(inside, newStart, isRaw);
            if (newEnd == -1) {
                throw ParserException.of("Unmatched braces in f-string", info);
            }
            var fmtPair = FormatInfo.parse(inside, newStart, newEnd - 1, info);
            formats.add(fmtPair.getKey());
            int end = newEnd - fmtPair.getValue();
            tests.add(parseTest(info, inside.substring(newStart + 1, end - 1)));
        }
        if (newEnd < inside.length()) {
            strings.add(maybeProcessEscapes(isRaw, inside.substring(newEnd), info));
        }
        return new FormattedStringNode(info, strings.toArray(new String[0]), tests.toArray(new TestNode[0]),
                formats.toArray(new FormatInfo[0]), prefixes);
    }

    /**
     * Parse the section of the string inside braces into a {@link TestNode}.
     *
     * @param info The line info for the string
     * @param section The section to parse
     * @return The freshly parsed TestNode
     */
    private static TestNode parseTest(LineInfo info, String section) {
        TokenList tokens = Tokenizer.parse(section, info.getPath(), info.getLineNumber());
        TestNode test;
        try {
            test = TestNode.parse(tokens);
        } catch (ParserException e) {
            throw ParserException.of(e.getInternalMessage(), info);
        }
        if (!tokens.tokenIs(TokenType.EPSILON)) {
            throw ParserException.of("Unexpected " + tokens.getFirst(), info);
        }
        return test;
    }

    /**
     * Get the size of the brace at the index in the string.
     *
     * @param str The string to find the brace in
     * @param start The index to start at
     * @return The position one after the final brace.
     */
    private static int sizeOfBrace(@NotNull String str, int start, boolean isRaw) {
        assert str.charAt(start) == '{' && isRaw || !isEscaped(str, start);
        int netBraces = 0;
        for (int index = start; index < str.length(); index++) {
            switch (str.charAt(index)) {
                case '{':
                    netBraces++;
                    break;
                case '}':
                    if (isRaw || netBraces > 1 || !isEscaped(str, start)) {
                        netBraces--;
                    }
                    break;
            }
            if (netBraces == 0) {
                return index + 1;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    private static boolean isEscaped(String str, int start) {
        for (int i = start - 1; i >= 0; i--) {
            if (str.charAt(i) != '\\') {
                return (start - i) % 2 == 0;
            }
        }
        return start % 2 == 1;
    }

    @Contract("true, _, _ -> param2; false, _, _ -> !null")
    private static String maybeProcessEscapes(boolean isRaw, String string, LineInfo info) {
        return isRaw ? string : processEscapes(string, info);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StringPrefix s : getPrefixes()) {
            sb.append(s.value);
        }
        sb.append('"');
        for (int i = 0; i < tests.length; i++) {
            sb.append(strings[i]);
            sb.append('{');
            sb.append(tests[i]);
            sb.append(formats[i]);
            sb.append('}');
        }
        for (int i = tests.length; i < strings.length; i++) {
            sb.append(strings[i]);
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * The class for formatting information for f-strings.
     *
     * @author Patrick Norton
     * @see FormattedStringNode
     */
    public static class FormatInfo {
        private static final Set<Character> FORMAT_INVALID = Set.of(
            '"', '\'', '[', ']', '(', ')', '{', '}'
        );
        private static final Set<Character> ALIGN_VALID = Set.of('>', '<', '=', '+');
        private static final Set<Character> SIGN_VALID = Set.of(' ', '+', '-');
        private static final Set<Character> TYPE_VALID = Set.of(
                'b', 'c', 'd', 'o', 'x', 'X', 'n', 'e', 'E', 'f', 'F', 'g', 'G', '%', 'r', 's'
        );

        // [[fill]align][sign][#][0][minimumwidth][.precision][type]
        private final char fill;
        private final char align;
        private final char sign;
        private final boolean hash;
        private final boolean zero;
        private final int minWidth;
        private final int precision;
        private final char type;

        private FormatInfo(
                char fill, char align, char sign, boolean hash, boolean zero, int minWidth, int precision, char type
        ) {
            this.fill = fill;
            this.align = align;
            this.sign = sign;
            this.hash = hash;
            this.zero = zero;
            this.minWidth = minWidth;
            this.precision = precision;
            this.type = type;
        }

        public char getFill() {
            return fill;
        }

        public char getAlign() {
            return align;
        }

        public char getSign() {
            return sign;
        }

        public boolean isHash() {
            return hash;
        }

        public boolean isZero() {
            return zero;
        }

        public int getMinWidth() {
            return minWidth;
        }

        public int getPrecision() {
            return precision;
        }

        public char getType() {
            return type;
        }

        public boolean isEmpty() {
            return onlyType() && type == '\0';
        }

        public boolean onlyType() {
            return fill == '\0' && align == '\0' && sign == '\0' && !hash
                    && !zero && minWidth == 0 && precision == 0;
        }

        public String toString() {
            if (isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder("!");
            if (fill != '\0') {
                sb.append(fill);
            }
            if (align != '\0') {
                sb.append(align);
            }
            if (sign != '\0') {
                sb.append(sign);
            }
            if (hash) {
                sb.append('#');
            }
            if (zero) {
                sb.append('0');
            }
            if (minWidth != 0) {
                sb.append(minWidth);
            }
            if (precision != 0) {
                sb.append(precision);
            }
            if (type != '\0') {
                sb.append(type);
            }
            return sb.toString();
        }

        static Pair<FormatInfo, Integer> parse(String str, int openBrace, int closeBrace, LineInfo lineInfo) {
            var specifier = specifier(str, openBrace, closeBrace);
            if (specifier == null) {
                return Pair.of(empty(), 0);
            } else if (specifier.isEmpty()) {
                return Pair.of(empty(), 1);
            }
            int currentPos = 0;
            char fill;
            char align;
            if (specifier.length() > 1 && ALIGN_VALID.contains(specifier.charAt(currentPos + 1))) {
                fill = specifier.charAt(currentPos);
                currentPos++;
                align = specifier.charAt(currentPos);
                currentPos++;
            } else {
                fill = '\0';
                if (ALIGN_VALID.contains(specifier.charAt(currentPos))) {
                    align = specifier.charAt(currentPos);
                    currentPos++;
                } else {
                    align = '\0';
                }
            }
            char sign;
            if (SIGN_VALID.contains(specifier.charAt(currentPos))) {
                sign = specifier.charAt(currentPos);
                currentPos++;
            } else {
                sign = '\0';
            }
            boolean hash;
            if (specifier.charAt(currentPos) == '#') {
                currentPos++;
                hash = true;
            } else {
                hash = false;
            }
            boolean zero;
            if (specifier.charAt(currentPos) == '0') {
                currentPos++;
                zero = true;
            } else {
                zero = false;
            }
            var min = parseInt(specifier.substring(currentPos));
            int minWidth = min.getKey();
            currentPos += min.getValue();
            int precision;
            if (specifier.charAt(currentPos) == '.') {
                currentPos++;
                var prec = parseInt(specifier.substring(currentPos));
                precision = prec.getKey();
                currentPos += prec.getValue();
            } else {
                precision = 0;
            }
            char type;
            if (TYPE_VALID.contains(specifier.charAt(currentPos))) {
                type = specifier.charAt(currentPos);
                currentPos++;
            } else {
                type = '\0';
            }
            if (currentPos != specifier.length()) {
                throw ParserException.of(String.format("Invalid format specifier !%s", specifier), lineInfo);
            }
            var len = specifier.length() + 1;
            return Pair.of(new FormatInfo(fill, align, sign, hash, zero, minWidth, precision, type), len);
        }

        @Nullable
        private static String specifier(String str, int openBrace, int closeBrace) {
            StringBuilder sb = new StringBuilder();
            for (int i = closeBrace - 1; i > openBrace; i--) {
                char currentChar = str.charAt(i);
                if (currentChar < 32 || currentChar >= 127 || FORMAT_INVALID.contains(currentChar)) {
                    return null;
                } else if (currentChar == '!') {
                    return str.charAt(i + 1) == '=' || str.charAt(i - 1) == '!' ? null : sb.reverse().toString();
                } else {
                    sb.append(currentChar);
                }
            }
            return null;
        }

        private static Pair<Integer, Integer> parseInt(String str) {
            int current = 0;
            int result = 0;
            while (current < str.length() && Character.isDigit(str.charAt(current))) {
                result *= 10;
                result += Character.digit(str.charAt(current), 10);
                current++;
            }
            return Pair.of(result, current);
        }

        static FormatInfo empty() {
            return new FormatInfo(
                    '\0', '\0', '\0', false, false, 0, 0, '\0'
            );
        }
    }
}
