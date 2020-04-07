package main.java.parser;

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
            formats.add(FormatInfo.parse(inside, newStart, newEnd - 1));
            int end = newEnd - formats.get(formats.size() - 1).size();
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
        TokenList tokens = Tokenizer.parse(section);
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

        private String specifier;

        @Contract(pure = true)
        FormatInfo(String specifier) {
            this.specifier = specifier;
        }

        @Contract(pure = true)
        public int size() {
            return specifier == null ? 0 : specifier.length() + 1;
        }

        public String getSpecifier() {
            return specifier;
        }

        /**
         * Get the format specifier out of a f-string.
         * <p>
         *     F-string specifiers always start with an !, and that is how they
         *     are parsed. (More formally, and for reference later, they go
         *     <code> "!" [conversion] [":" [see <a href=
         *     https://www.python.org/dev/peps/pep-3101>Python's formatting
         *     grammar</a>]]</code>).
         * </p>
         *
         * @param str The string to be parsed
         * @param openBrace The location of the open brace
         * @param closeBrace The location of the close brace
         * @return The parsed specifier
         * @see <a href=https://www.python.org/dev/peps/pep-3101>
         *     Python's formatting grammar</a>
         */
        @NotNull
        @Contract("_, _, _ -> new")
        static FormatInfo parse(String str, int openBrace, int closeBrace) {
            String specifier = specifier(str, openBrace, closeBrace);
            return new FormatInfo(specifier);
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

        public String toString() {
            return specifier == null ? "" : " !" + specifier;
        }
    }

}
