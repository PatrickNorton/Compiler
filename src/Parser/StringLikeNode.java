package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The abstract class representing a StringNode or a FormattedStringNode.
 *
 * @author Patrick Norton
 */
public abstract class StringLikeNode implements AtomicNode {
    static final Pattern PREFIXES = Pattern.compile("^[refb]*");
    static final Pattern CONTENT = Pattern.compile("(^[refb]*\")|(\"$)");

    private LineInfo lineInfo;
    private Set<StringPrefix> prefixes;

    @Contract(pure = true)
    public StringLikeNode(LineInfo lineInfo, String prefixes) {
        this.prefixes = StringPrefix.getPrefixes(prefixes);
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
}
