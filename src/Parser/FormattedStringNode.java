package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

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

    /**
     * Construct a new FormattedStringNode.
     * @param strings The intermittent string literals
     * @param tests The non-string-literals which are interpolated
     */
    @Contract(pure = true)
    public FormattedStringNode(LineInfo info, String[] strings, TestNode[] tests, @NotNull String flags) {
        super(info, flags);
        this.strings = strings;
        this.tests = tests;
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
        String inside = CONTENT.matcher(token.sequence).replaceAll("");
        Matcher prefixMatcher = PREFIXES.matcher(token.sequence);
        String prefixes;
        if (prefixMatcher.find()) {
            prefixes = prefixMatcher.group();
        } else {
            throw ParserException.of("Prefix-finding match should not have failed", token);
        }
        boolean isRaw = prefixes.contains("r");
        List<String> strings = new ArrayList<>();
        List<TestNode> tests = new ArrayList<>();
        int newStart, newEnd = 0;
        while ((newStart = inside.indexOf('{', newEnd)) != -1) {
            if (inside.charAt(newStart - 1) == '\\') {
                continue;
            }
            strings.add(inside.substring(newEnd, newStart));
            newEnd = sizeOfBrace(inside, newStart, isRaw);
            if (newEnd == -1) {
                throw ParserException.of("Unmatched braces in f-string", info);
            }
            tests.add(parseTest(info, inside.substring(newStart + 1, newEnd - 1)));
        }
        if (newEnd < inside.length()) {
            strings.add(maybeProcessEscapes(isRaw, inside.substring(newEnd), info));
        }
        return new FormattedStringNode(info, strings.toArray(new String[0]), tests.toArray(new TestNode[0]), prefixes);
    }

    /**
     * Parse the section of the string inside braces into a {@link TestNode}.
     *
     * @param info The line info for the string
     * @param section The section to parse
     * @return The freshly parsed TestNode
     */
    private static TestNode parseTest(LineInfo info, String section) {
        TokenList tokenList = Tokenizer.parse(section);
        TestNode test;
        try {
            test = TestNode.parse(tokenList);
        } catch (ParserException e) {
            throw ParserException.of(e.getInternalMessage(), info);
        }
        if (!tokenList.tokenIs(TokenType.EPSILON)) {
            throw ParserException.of("Unexpected " + tokenList.getFirst(), info);
        }
        return test;
    }

    /**
     * Get the size of the brace at the index in the string.
     *
     * @param str The string to find the brace in
     * @param index The index to start at
     * @return The position one after the final brace.
     */
    private static int sizeOfBrace(@NotNull String str, int index, boolean isRaw) {
        assert str.charAt(index) == '{';
        int netBraces = 0;
        while (index < str.length()) {
            switch (str.charAt(index++)) {
                case '{':
                    netBraces++;
                    break;
                case '}':
                    if (!isRaw || netBraces > 1 || str.charAt(index - 2) != '\\') {
                        netBraces--;
                    }
                    break;
            }
            if (netBraces == 0) {
                return index;
            }
        }
        return -1;
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
            sb.append('}');
        }
        for (int i = tests.length; i < strings.length; i++) {
            sb.append(strings[i]);
        }
        sb.append('"');
        return sb.toString();
    }
}
