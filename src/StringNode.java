import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class representing a string literal.
 * <p>
 *     This does not represent formatted strings, as they require special
 *     syntax. For those, see {@link FormattedStringNode}.
 * </p>
 * @author Patrick Norton
 * @see FormattedStringNode
 */
public class StringNode implements AtomicNode {
    private String contents;
    private char[] prefixes;  // TODO? Enumerate string prefixes

    @Contract(pure = true)
    public StringNode(String contents, char... prefixes) {
        this.contents = contents;
        this.prefixes = prefixes;
    }

    public String getContents() {
        return contents;
    }

    public char[] getPrefixes() {
        return prefixes;
    }

    /**
     * Parse a StringNode from a list of tokens.
     * <p>
     *     String nodes consist only of a string token, and thus have only the
     *     requirement that the first node of the token list is of type STRING.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed string literal
     */
    @NotNull
    @Contract("_ -> new")
    static AtomicNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.STRING);
        String contents = tokens.getFirst().sequence;
        tokens.nextToken();
        String inside = contents.replaceAll("(^[rfb]*\")|(?<!\\\\)\"", "");
        Matcher regex = Pattern.compile("^[rfb]+").matcher(contents);
        if (regex.find()) {
            String prefixes = regex.group();
            if (!prefixes.contains("r")) {
                inside = processEscapes(inside);
            }
            if (prefixes.contains("f")) {
                LinkedList<String> strs = new LinkedList<>();
                LinkedList<TestNode> tests = new LinkedList<>();
                Matcher m = Pattern.compile("(?<!\\\\)(\\{([^{}]*)}?|})").matcher(inside);
                int index = 0;
                int start, end = 0;
                while (m.find()) {  // TODO: Find better way of handling this and/or refactor into FormattedStringNode
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
                        throw new ParserException("Unmatched braces in "+tokens.getFirst().sequence);
                    }
                    end = m.end();
                    LinkedList<Token> tokenList = Tokenizer.parse(to_test.substring(1, to_test.length() - 1)).getTokens();
                    tokenList.add(new Token(TokenType.EPSILON, ""));
                    TokenList newTokens = new TokenList(tokenList);
                    tests.add(TestNode.parse(newTokens));
                    if (!newTokens.tokenIs(TokenType.EPSILON)) {
                        throw new ParserException("Unexpected " + newTokens.getFirst());
                    }
                    index = end + 1;
                }
                if (index <= inside.length()) {
                    strs.add(inside.substring(end));
                }
                return new FormattedStringNode(strs.toArray(new String[0]), tests.toArray(new TestNode[0]));
            }
            return new StringNode(inside, prefixes.toCharArray());
        }
        inside = processEscapes(inside);
        return new StringNode(inside);
    }

    /**
     * Process the escape sequences for the string.
     * @param str The string to be processed
     * @return The escaped string
     */
    @NotNull
    private static String processEscapes(@NotNull String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if (chr != '\\') {
                sb.append(chr);
                continue;
            }
            char chr2 = str.charAt(i+1);
            switch (chr2) {
                case '\\':
                    sb.append('\\');
                    break;
                case '"':
                    sb.append('"');
                    break;
                case '\'':
                    sb.append('\'');
                    break;
                case 'a':
                    sb.append('\7');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'v':
                    sb.append('\013');
                    break;
                case 'o':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 4), 8));
                    i += 3;
                    break;
                case 'x':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 3), 16));
                    i += 2;
                    break;
                case 'u':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 5), 16));
                    i += 4;
                    break;
                case 'U':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 9), 16));
                    i += 8;
                    break;
                default:
                    throw new ParserException("Unknown escape sequence " + str.substring(i, i+1));
            }
            i++;
        }
        return sb.toString();
    }
}
