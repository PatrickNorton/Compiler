package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a container literal.
 * <p>
 *     This class does not include dictionary literals, due to their special
 *     syntax, for that see {@link DictLiteralNode}.
 * </p>
 * @author Patrick Norton
 * @see DictLiteralNode
 */
public class LiteralNode implements SubTestNode, PostDottableNode {
    private LineInfo lineInfo;
    private String braceType;
    private TestNode[] builders;
    private String[] isSplats;

    @Contract(pure = true)
    public LiteralNode(LineInfo lineInfo, String braceType, TestNode[] builders, String[] isSplats) {
        this.lineInfo = lineInfo;
        this.braceType = braceType;
        this.builders = builders;
        this.isSplats = isSplats;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getBraceType() {
        return braceType;
    }

    public TestNode[] getBuilders() {
        return builders;
    }

    public String[] getIsSplats() {
        return isSplats;
    }

    /**
     * Parse a container literal from a list of tokens.
     * <p>
     *     The syntax for a container literal is: <code>OPEN_BRACE [["*"]
     *     {@link TestNode} *("," ["*"] {@link TestNode}) [","]]
     *     MATCHING_BRACE</code>. The list of tokens must begin with an open
     *     brace when passed here.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The newly parsed LiteralNode
     */
    @NotNull
    @Contract("_ -> new")
    static LiteralNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        LineInfo lineInfo = tokens.lineInfo();
        String braceType = tokens.tokenSequence();
        String matchingBrace = tokens.matchingBrace();
        tokens.nextToken(true);
        LinkedList<TestNode> values = new LinkedList<>();
        LinkedList<String> isSplat = new LinkedList<>();
        while (!tokens.tokenIs(matchingBrace)) {
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw tokens.error("Unmatched braces");
            }
            if (tokens.tokenIs("*", "**")) {
                isSplat.add(tokens.tokenSequence());
                tokens.nextToken(true);
            } else {
                isSplat.add("");
            }
            values.add(TestNode.parse(tokens, true));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(true);
            } else {
                break;
            }
        }
        if (tokens.tokenIs(matchingBrace)) {
            tokens.nextToken();
        } else {
            throw tokens.error("Unmatched braces");
        }
        return new LiteralNode(lineInfo, braceType, values.toArray(new TestNode[0]), isSplat.toArray(new String[0]));

    }

    @Override
    public String toString() {
        String endBrace =  TokenList.matchingBrace(braceType);
        switch (builders.length) {
            case 0:
                return braceType + endBrace;
            case 1:
                return braceType + isSplats[0] + builders[0] + (braceType.equals("(") ? "," : "") + endBrace;
            default:
                return String.format("%s%s%s, ...%s", braceType, isSplats[0], builders[0], endBrace);
        }
    }
}
