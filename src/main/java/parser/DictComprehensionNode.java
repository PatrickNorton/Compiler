package main.java.parser;

import main.java.util.Pair;

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

    public DictComprehensionNode(LineInfo lineInfo, TestNode key, TestNode val,
                                 VarLikeNode[] vars, TestListNode looped, TestNode condition,
                                 TestNode whileCond) {
        super(lineInfo, "{", vars, val, looped, condition, whileCond);
        this.key = key;
    }

    public TestNode getKey() {
        return key;
    }

    /**
     * Parse a DictComprehensionNode from a list of tokens.
     *
     * <p>
     *     The syntax for a dictionary comprehension is: <code>"{" {@link
     *     TestNode} ":" {@link TestNode} "for" {@link VarLikeNode}
     *     *("," {@link VarLikeNode}) [","] "in" {@link TestNode}
     *     *("," {@link TestNode}) [","] ["if" {@link TestNode}] ["while"
     *     {@link TestNode}] "}"</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly created DictComprehensionNode
     * @see ComprehensionNode#parse
     */

    static DictComprehensionNode parse(TokenList tokens) {
        assert tokens.tokenIs("{");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        TestNode key = TestNode.parse(tokens, true);
        tokens.expect(":", true);
        TestNode val = TestNode.parse(tokens, true);
        tokens.expect(Keyword.FOR, true);
        VarLikeNode[] vars = VarLikeNode.parseList(tokens, true);
        tokens.expect(Keyword.IN, true);
        Pair<TestListNode, TestNode> loopedAndCondition = TestListNode.parsePostIf(tokens, true);
        TestListNode looped = loopedAndCondition.getKey();
        TestNode condition = loopedAndCondition.getValue();
        TestNode whileCond = TestNode.parseOnToken(tokens, Keyword.WHILE, true);
        tokens.expect("}");
        return new DictComprehensionNode(info, key, val, vars, looped, condition, whileCond);
    }

    @Override
    public String toString() {
        return String.format("{%s: %s%s", ArgumentNode.toString(getBuilder()), key, secondHalfString());
    }
}
