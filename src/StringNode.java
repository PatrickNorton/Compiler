import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.regex.Matcher;

/**
 * The class representing a string literal.
 * <p>
 *     This does not represent formatted strings, as they require special
 *     syntax. For those, see {@link FormattedStringNode}.
 * </p>
 * @author Patrick Norton
 * @see FormattedStringNode
 */
public class StringNode extends StringLikeNode {
    private String contents;
    private StringPrefix[] prefixes;

    @Contract(pure = true)
    public StringNode(String contents, @NotNull char... prefixes) {
        this.contents = contents;
        LinkedList<StringPrefix> stringPrefixes = new LinkedList<>();
        for (char c : prefixes) {
            stringPrefixes.add(StringPrefix.getPrefix(c));
        }
        this.prefixes = stringPrefixes.toArray(new StringPrefix[0]);
    }

    public String getContents() {
        return contents;
    }

    public StringPrefix[] getPrefixes() {
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
    static StringLikeNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.STRING);
        String contents = tokens.getFirst().sequence;
        tokens.nextToken();
        String inside = contentPattern.matcher(contents).replaceAll("");
        Matcher regex = prefixPattern.matcher(contents);
        if (regex.find()) {
            String prefixes = regex.group();
            if (!prefixes.contains("r")) {
                inside = processEscapes(inside);
            }
            if (prefixes.contains("f")) {
                return FormattedStringNode.parse(tokens, contents);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StringPrefix s : prefixes) {
            sb.append(s);
        }
        return sb + contents;
    }
}
