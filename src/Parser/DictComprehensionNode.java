package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

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
public class DictComprehensionNode implements SubTestNode {
    private LineInfo lineInfo;
    private TestNode key;
    private TestNode val;
    private TypedVariableNode[] vars;
    private TestNode[] looped;

    /**
     * Create new instance of DictComprehensionNode.
     * @param key The value which is evaluated to create the dict keys
     * @param val The value which is evaluated to create the corresponding
     *            value
     * @param vars The variables which are being incremented
     * @param looped The values being looped over
     */
    @Contract(pure = true)
    public DictComprehensionNode(LineInfo lineInfo, TestNode key, TestNode val, TypedVariableNode[] vars, TestNode[] looped) {
        this.lineInfo = lineInfo;
        this.key = key;
        this.val = val;
        this.vars = vars;
        this.looped = looped;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getKey() {
        return key;
    }

    public TestNode getVal() {
        return val;
    }

    public TypedVariableNode[] getVars() {
        return vars;
    }

    public TestNode[] getLooped() {
        return looped;
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
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Expected }, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DictComprehensionNode(info, key, val, vars, looped);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TypedVariableNode t : vars) {
            sj.add(t.toString());
        }
        String vars = sj.toString();
        sj = new StringJoiner(", ");
        for (TestNode t : looped) {
            sj.add(t.toString());
        }
        return "{" + key + ": " + val + " for " + vars + " in " + sj;
    }
}
