package main.java.parser;

import java.util.Set;

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

    public StringNode(LineInfo lineInfo, String contents,Set<StringPrefix> prefixes) {
        super(lineInfo, prefixes);
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public String[] getStrings() {
        return new String[] {contents};
    }

    static StringNode parse(Token token) {
        assert token.is(TokenType.STRING);
        LineInfo lineInfo = token.lineInfo;
        String inside = getContents(token);
        Set<StringPrefix> prefixes = getPrefixes(token);
        assert !prefixes.contains(StringPrefix.FORMATTED);
        if (!prefixes.contains(StringPrefix.RAW)) {
            inside = processEscapes(inside, lineInfo);
        }
        return new StringNode(lineInfo, inside, prefixes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (StringPrefix s : getPrefixes()) {
            sb.append(s);
        }
        String newContents = contents;
        char delimiter = '"';
        if (contents.contains("\"")) {
            if (contents.contains("'")) {
                newContents = contents.replace("\"", "\\\"");
            } else {
                delimiter = '\'';
            }
        }
        return sb.append(delimiter).append(newContents).append(delimiter).toString();
    }
}
