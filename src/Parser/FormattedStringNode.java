package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class representing a formatted string.
 * <p>
 *     This is separate intentionally from the normal {@link StringNode},
 *     because it has a list of tests which need to be performed in order to be
 *     turned into a proper string.
 * </p>
 * @author Patrick Norton
 * @see StringNode
 */
public class FormattedStringNode extends StringLikeNode {
    private String[] strs;
    private TestNode[] tests;

    private static final Pattern bracePattern = Pattern.compile("(?<!\\\\)(\\{([^{}]*)}?|})");

    /**
     * Construct a new FormattedStringNode.
     * @param strs The intermittent string literals
     * @param tests The non-string-literals which are interpolated
     */
    @Contract(pure = true)
    public FormattedStringNode(LineInfo info, String[] strs, TestNode[] tests, @NotNull String flags) {
        super(info, flags);
        this.strs = strs;
        this.tests = tests;
    }

    @Override
    public String[] getStrings() {
        return strs;
    }

    public TestNode[] getTests() {
        return tests;
    }

    /**
     * Parse a FormattedStringNode from a {@link String} representing its
     * contents.
     * @param token The token for the contents of the string
     * @return The freshly parsed FormattedStringNode
     */
    @NotNull
    @Contract("_ -> new")
    static FormattedStringNode parse(@NotNull Token token) {
        LineInfo info = token.lineInfo;
        String contents = token.sequence;
        String inside = CONTENT.matcher(contents).replaceAll("");
        Matcher prefixMatcher = PREFIXES.matcher(contents);
        String prefixes;
        if (prefixMatcher.find()) {
            prefixes = prefixMatcher.group();
        } else {
            throw ParserException.of("Match should not have failed", token);
        }
        boolean isRaw = prefixes.contains("r");
        LinkedList<String> strs = new LinkedList<>();
        LinkedList<TestNode> tests = new LinkedList<>();
        // Match the inside-brace portions of the string
        Matcher m = bracePattern.matcher(inside);
        int index = 0;
        int start, end = 0;
        while (m.find()) {
            start = m.start();
            strs.add(maybeProcessEscapes(isRaw, inside.substring(index, start - 1), info));
            StringBuilder to_test = new StringBuilder();
            int netBraces = 0;
            /*
             * Since Java doesn't allow recursion in regex, this uses
             * bracePattern, which matches curly braces either at the beginning
             * or end of the match, and then just keep matching that until the
             * net number of braces is zero.
             */
            do {
                String a = m.group();
                to_test.append(a);
                if (a.startsWith("{")) {
                    netBraces++;
                }
                if (a.endsWith("}")) {
                    netBraces--;
                }
            } while (netBraces > 0 && m.find());
            if (netBraces > 0) {
                throw ParserException.of("Unmatched braces in " + inside, token);
            }
            end = m.end();
            TokenList tokenList = Tokenizer.parse(to_test.substring(1, to_test.length() - 1));
            try {
                tests.add(TestNode.parse(tokenList));
            } catch (ParserException e) {
                throw ParserException.of(e.getInternalMessage(), info);
            }
            if (!tokenList.tokenIs(TokenType.EPSILON)) {
                throw ParserException.of("Unexpected " + tokenList.getFirst(), token);
            }
            index = end + 1;
        }
        if (index <= inside.length()) {
            strs.add(maybeProcessEscapes(isRaw, inside.substring(end), info));
        }
        return new FormattedStringNode(info, strs.toArray(new String[0]), tests.toArray(new TestNode[0]), prefixes);
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
            sb.append(strs[i]);
            sb.append('{');
            sb.append(tests[i]);
            sb.append('}');
        }
        if (strs.length > tests.length) {
            for (int i = tests.length; i < strs.length; i++) {
                sb.append(strs[i]);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
