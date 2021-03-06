package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
    @Contract(pure = true)
    public StringNode(LineInfo lineInfo, String contents, @NotNull Set<StringPrefix> prefixes) {
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

    @NotNull
    @Contract("_ -> new")
    static StringNode parse(@NotNull Token token) {
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
