package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The class representing a dictionary literal.
 * <p>
 *     This class is separate from {@link LiteralNode} because it has a slightly
 *     differing syntax and nodal representation. See also the difference
 *     between {@link ComprehensionNode} and {@link DictComprehensionNode}.
 * </p>
 * @author Patrick Norton
 * @see LiteralNode
 * @see DictComprehensionNode
 */
public class DictLiteralNode implements SubTestNode, PostDottableNode {
    private LineInfo lineInfo;
    private TestNode[] keys;
    private TestNode[] values;

    @Contract(pure = true)
    public DictLiteralNode(LineInfo info) {
        this(info, new TestNode[0], new TestNode[0]);
    }

    @Contract(pure = true)
    public DictLiteralNode(LineInfo info, TestNode[] keys, TestNode[] values) {
        this.lineInfo = info;
        this.keys = keys;
        this.values = values;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode[] getKeys() {
        return keys;
    }

    public TestNode[] getValues() {
        return values;
    }

    /**
     * Parse a DictLiteralNode from a list of tokens.
     * <p>
     *     The syntax of a dictionary literal is as follows: <code>"{" {@link
     *     TestNode} ":" {@link TestNode} *("," {@link TestNode} ":" {@link
     *     TestNode}) [","] "}"</code>. This is different from other
     *     container literals.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed DictLiteralNode
     */
    @NotNull
    @Contract("_ -> new")
    static DictLiteralNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("{");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        List<TestNode> keys = new ArrayList<>();
        List<TestNode> values = new ArrayList<>();
        if (tokens.tokenIs(":")) {
            tokens.nextToken(true);
            if (!tokens.tokenIs("}")) {
                throw tokens.error("Expected }, got " + tokens.getFirst());
            }
            tokens.nextToken();
            return new DictLiteralNode(info);
        }
        while (true) {
            keys.add(TestNode.parse(tokens));
            if (!tokens.tokenIs(":")) {
                throw tokens.error("Dict comprehension must have colon");
            }
            tokens.nextToken(true);
            values.add(TestNode.parse(tokens));
            if (!tokens.tokenIs(",")) {
                break;
            }
            tokens.nextToken(true);
            if (tokens.tokenIs("}")) {
                break;
            }
        }
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Unmatched brace");
        }
        tokens.nextToken();
        return new DictLiteralNode(info, keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }

    @Override
    public String toString() {
        switch (keys.length) {
            case 0:
                return "{:}";
            case 1:
                return String.format("{%s: %s}", keys[0], values[0]);
            default:
                return String.format("{%s: %s, ...}", keys[0], values[0]);
        }
    }
}
