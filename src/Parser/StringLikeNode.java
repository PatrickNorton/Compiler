package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The abstract class representing a StringNode or a FormattedStringNode.
 *
 * @author Patrick Norton
 */
public abstract class StringLikeNode implements AtomicNode {
    private LineInfo lineInfo;
    private Set<StringPrefix> prefixes;

    @Contract(pure = true)
    StringLikeNode(LineInfo lineInfo, Set<StringPrefix> prefixes) {
        this.prefixes = prefixes;
        this.lineInfo = lineInfo;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public Set<StringPrefix> getPrefixes() {
        return prefixes;
    }

    public abstract String[] getStrings();

    /**
     * Parse a StringLikeNode from a list of tokens.
     * <p>
     *     String nodes consist only of a string token, and thus have only the
     *     requirement that the first node of the token list is of type STRING.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed string literal
     */
    @NotNull
    static StringLikeNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.STRING);
        Token token = tokens.getFirst();
        tokens.nextToken();
        if (getPrefixes(token).contains(StringPrefix.FORMATTED)) {
            return FormattedStringNode.parse(token);
        } else {
            return StringNode.parse(token);
        }
    }

    /**
     * Process the escape sequences for the string.
     * @param str The string to be processed
     * @return The escaped string
     */
    @NotNull
    static String processEscapes(@NotNull String str, LineInfo info) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if (chr != '\\') {
                sb.append(chr);
                continue;
            }
            char chr2 = str.charAt(++i);
            switch (chr2) {
                case '\\':
                    sb.append('\\');
                    break;
                case '"':
                    sb.append('"');
                    break;
                case '{':
                    sb.append('{');
                    break;
                case '}':
                    sb.append('}');
                    break;
                case '\'':
                    sb.append('\'');
                    break;
                case '0':
                    sb.append('\0');
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
                    sb.append(Integer.parseInt(str.substring(i + 1, i + 4), 8));
                    i += 4;
                    break;
                case 'x':
                    sb.append(Integer.parseInt(str.substring(i + 1, i + 3), 16));
                    i += 3;
                    break;
                case 'u':
                    sb.append(Integer.parseInt(str.substring(i + 1, i + 5), 16));
                    i += 5;
                    break;
                case 'U':
                    sb.append(Integer.parseInt(str.substring(i + 1, i + 9), 16));
                    i += 9;
                    break;
                default:
                    throw ParserException.of("Unknown escape sequence " + str.substring(i, i+1), info);
            }
        }
        return sb.toString();
    }

    /**
     * Get the number of prefixes on the beginning of a string.
     * <p>
     *     This assumes the string given is a well-formatted string literal.
     * </p>
     *
     * @param sequence The string to count the prefixes of
     * @return The prefix count
     */
    private static int prefixCount(@NotNull String sequence) {
        return sequence.indexOf(delimiter(sequence));
    }

    /**
     * Get the delimiter of the string literal.
     * <p>
     *     This assumes the string given is a well-formatted string literal.
     * </p>
     *
     * @param sequence The string to get the delimiter fo
     * @return The delimiter
     */
    private static char delimiter(@NotNull String sequence) {
        return sequence.charAt(sequence.length() - 1);
    }

    /**
     * Get the prefixes from a string literal token.
     *
     * @param token The token to be prefixed
     * @return The prefixes of the token
     */
    @NotNull
    static Set<StringPrefix> getPrefixes(@NotNull Token token) {
        assert token.is(TokenType.STRING);
        String sequence = token.sequence;
        String prefixes = sequence.substring(0, prefixCount(sequence));
        return StringPrefix.getPrefixes(prefixes);
    }

    /**
     * Get the text withing the quotes of a string literal token.
     *
     * @param token The token from which to get the contents
     * @return The contents of the token
     */
    @NotNull
    static String getContents(@NotNull Token token) {
        assert token.is(TokenType.STRING);
        String sequence = token.sequence;
        return sequence.substring(prefixCount(sequence) + 1, sequence.length() - 1);
    }
}
