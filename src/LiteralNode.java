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
    private String brace_type;
    private TestNode[] builders;
    private Boolean[] is_splats;

    @Contract(pure = true)
    public LiteralNode(String brace_type, TestNode[] builders, Boolean[] is_splats) {
        this.brace_type = brace_type;
        this.builders = builders;
        this.is_splats = is_splats;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TestNode[] getBuilders() {
        return builders;
    }

    public Boolean[] getIs_splats() {
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
        String brace_type = tokens.getFirst().sequence;
        tokens.nextToken(true);
        String balanced_brace = TokenList.matchingBrace(brace_type);
        LinkedList<TestNode> values = new LinkedList<>();
        LinkedList<Boolean> is_splat = new LinkedList<>();
        while (true) {
            if (tokens.tokenIs(balanced_brace)) {
                break;
            } else if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            if (tokens.tokenIs("*")) {
                is_splat.add(true);
                tokens.nextToken(true);
            } else {
                is_splat.add(false);
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
            throw new ParserException("Unmatched braces");
        }
        return new LiteralNode(brace_type, values.toArray(new TestNode[0]), is_splat.toArray(new Boolean[0]));

    }

    @Override
    public String toString() {
        if (builders.length == 0) {
            return brace_type + TokenList.matchingBrace(brace_type);
        } else {
            return brace_type + (is_splats[0] ? "*" : "") + TokenList.matchingBrace(brace_type);
        }
    }
}
