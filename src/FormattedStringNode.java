import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class representing a formatted string.
 * <p>
 *     This is separate intentionally from the normal {@link StringNode},
 *     because it has a list of tests which need to be performed in order to be
 *     turned into a proper string. There is no parse method for this (yet?), as
 *     all parsing is done instead by the normal {@link StringNode#parse}.
 * </p>
 * @author Patrick Norton
 * @see StringNode
 */
public class FormattedStringNode extends StringLikeNode {
    private String[] strs;
    private TestNode[] tests;
    private StringPrefix[] prefixes;

    private static final Pattern bracePattern = Pattern.compile("(?<!\\\\)(\\{([^{}]*)}?|})");

    /**
     * Construct a new FormattedStringNode.
     * @param strs The intermittent string literals
     * @param tests The non-string-literals which are interpolated
     */
    @Contract(pure = true)
    public FormattedStringNode(String[] strs, TestNode[] tests, char[] flags) {
        this.strs = strs;
        this.tests = tests;
        ArrayList<StringPrefix> prefixes = new ArrayList<>();
        for (char c : flags) {
            prefixes.add(StringPrefix.getPrefix(c));
        }
        this.prefixes = prefixes.toArray(new StringPrefix[0]);
    }

    public String[] getStrs() {
        return strs;
    }

    public TestNode[] getTests() {
        return tests;
    }

    public StringPrefix[] getPrefixes() {
        return prefixes;
    }

    @NotNull
    @Contract("_, _ -> new")
    static AtomicNode parse(TokenList tokens, @NotNull String contents) {
        String inside = contentPattern.matcher(contents).replaceAll("");
        Matcher prefixMatcher = prefixPattern.matcher(contents);
        String prefixes;
        if (prefixMatcher.find()) {
            prefixes = prefixMatcher.group();
        } else {
            throw new ParserException("Match should not have failed");
        }
        LinkedList<String> strs = new LinkedList<>();
        LinkedList<TestNode> tests = new LinkedList<>();
        Matcher m = bracePattern.matcher(inside);
        int index = 0;
        int start, end = 0;
        while (m.find()) {
            start = m.start();
            strs.add(inside.substring(index, start - 1));
            StringBuilder to_test = new StringBuilder();
            int netBraces = 0;
            do {
                String a = m.group();
                to_test.append(a);
                if (a.startsWith("{")) netBraces++;
                if (a.endsWith("}")) netBraces--;
                if (netBraces == 0) break;
            } while (m.find());
            if (netBraces > 0) {
                throw new ParserException("Unmatched braces in " + inside);
            }
            end = m.end();
            TokenList tokenList = Tokenizer.parse(to_test.substring(1, to_test.length() - 1));
            tests.add(TestNode.parse(tokenList));
            if (!tokenList.tokenIs(TokenType.EPSILON)) {
                throw new ParserException("Unexpected " + tokenList.getFirst());
            }
            index = end + 1;
        }
        if (index <= inside.length()) {
            strs.add(inside.substring(end));
        }
        FormattedStringNode fString = new FormattedStringNode(strs.toArray(new String[0]),
                tests.toArray(new TestNode[0]), prefixes.toCharArray());
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, fString);
        } else {
            return fString;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StringPrefix s : prefixes) {
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
