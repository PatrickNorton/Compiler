package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
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

    /**
     * Create a new instance of StringNode.
     * @param contents The contents of the string
     * @param prefixes The prefixes thereof
     */
    @Contract(pure = true)
    public StringNode(LineInfo lineInfo, String contents, @NotNull String prefixes) {
        super(lineInfo, prefixes);
        this.contents = contents;
    }

    public StringNode(LineInfo lineInfo, String contents) {
        this(lineInfo, contents, "");
    }

    public String getContents() {
        return contents;
    }

    @Override
    public String[] getStrings() {
        return new String[] {contents};
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
        Token token = tokens.getFirst();
        LineInfo lineInfo = token.lineInfo;
        String contents = token.sequence;
        tokens.nextToken();
        String inside = contentPattern.matcher(contents).replaceAll("");
        Matcher regex = prefixPattern.matcher(contents);
        if (regex.find()) {
            String prefixes = regex.group();
            if (!prefixes.contains("r")) {
                inside = processEscapes(inside, token.lineInfo);
            }
            if (prefixes.contains("f")) {
                return FormattedStringNode.parse(token);
            }
            return new StringNode(lineInfo, inside, prefixes);
        }
        inside = processEscapes(inside, token.lineInfo);
        return new StringNode(lineInfo, inside);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StringPrefix s : getPrefixes()) {
            sb.append(s);
        }
        return sb + contents;
    }
}
