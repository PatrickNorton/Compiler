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
    private String brace_type;
    private TestNode[] builders;
    private String[] is_splats;

    @Contract(pure = true)
    public LiteralNode(LineInfo lineInfo, String brace_type, TestNode[] builders, String[] is_splats) {
        this.brace_type = brace_type;
        this.builders = builders;
        this.is_splats = is_splats;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TestNode[] getBuilders() {
        return builders;
    }

    public String[] getIs_splats() {
        return is_splats;
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
        String brace_type = tokens.tokenSequence();
        String balanced_brace = tokens.matchingBrace();
        tokens.nextToken(true);
        LinkedList<TestNode> values = new LinkedList<>();
        LinkedList<String> is_splat = new LinkedList<>();
        while (!tokens.tokenIs(balanced_brace)) {
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw tokens.error("Unmatched braces");
            }
            if (tokens.tokenIs("*", "**")) {
                is_splat.add(tokens.tokenSequence());
                tokens.nextToken(true);
            } else {
                is_splat.add("");
            }
            values.add(TestNode.parse(tokens, true));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(true);
            } else {
                break;
            }
        }
        if (tokens.tokenIs(balanced_brace)) {
            tokens.nextToken();
        } else {
            throw tokens.error("Unmatched braces");
        }
        return new LiteralNode(lineInfo, brace_type, values.toArray(new TestNode[0]), is_splat.toArray(new String[0]));

    }

    @Override
    public String toString() {
        String endBrace =  TokenList.matchingBrace(brace_type);
        switch (builders.length) {
            case 0:
                return brace_type + endBrace;
            case 1:
                return brace_type + is_splats[0] + builders[0] + (brace_type.equals("(") ? "," : "") + endBrace;
            default:
                return String.format("%s%s%s, ...%s", brace_type, is_splats[0], builders[0], endBrace);
        }
    }
}
