package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a dictionary comprehension.
 * <p>
 *     This class is intentionally separate from {@link ComprehensionNode},
 *     because normal comprehensions do not have the key-value pairing that
 *     a dict comprehension does. See also the difference between {@link
 *     LiteralNode} and {@link DictLiteralNode}.
 * </p>
 * @author Patrick Norton
 * @see ComprehensionNode
 * @see DictLiteralNode
 */
public class DictComprehensionNode extends ComprehensionLikeNode {
    private TestNode key;

    /**
     * Create new instance of DictComprehensionNode.
     * @param key The value which is evaluated to create the dict keys
     * @param val The value which is evaluated to create the corresponding
     *            value
     * @param vars The variables which are being incremented
     * @param looped The values being looped over
     */
    @Contract(pure = true)
    public DictComprehensionNode(LineInfo lineInfo, TestNode key, TestNode val,
                                 TypedVariableNode[] vars, TestNode[] looped, TestNode condition) {
        super(lineInfo, "{", vars, val, looped, condition);
        this.key = key;
    }

    public TestNode getKey() {
        return key;
    }

    /**
     * Parse a DictComprehensionNode from a list of tokens.
     * <p>
     *     The syntax for a dictionary comprehension is: <code>"{" {@link
     *     TestNode} ":" {@link TestNode} "for" {@link TypedVariableNode}
     *     *("," {@link TypedVariableNode}) [","] "in" {@link TestNode}
     *     *("," {@link TestNode}) [","] "}"</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly created DictComprehensionNode
     * @see ComprehensionNode#parse
     */
    @NotNull
    @Contract("_ -> new")
    static DictComprehensionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("{");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        TestNode key = TestNode.parse(tokens, true);
        if (!tokens.tokenIs(":")) {
            throw tokens.error("Expected :, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        TestNode val = TestNode.parse(tokens, true);
        if (!tokens.tokenIs(Keyword.FOR)) {
            throw tokens.error("Expected for, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        TypedVariableNode[] vars = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs(Keyword.IN)) {
            throw tokens.error("Expected in, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        TestNode[] looped = TestNode.parseList(tokens, true);
        TestNode condition = TestNode.parseOnToken(tokens, Keyword.IF, true);
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Expected }, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DictComprehensionNode(info, key, val, vars, looped, condition);
    }

    @Override
    public String toString() {
        return String.format("{%s: %s%s", ArgumentNode.toString(getBuilder()), key, secondHalfString());
    }
}
