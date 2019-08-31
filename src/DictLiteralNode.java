import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

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
public class DictLiteralNode implements SubTestNode {
    private TestNode[] keys;
    private TestNode[] values;

    @Contract(pure = true)
    public DictLiteralNode(TestNode[] keys, TestNode[] values) {
        this.keys = keys;
        this.values = values;
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
        tokens.nextToken(true);
        LinkedList<TestNode> keys = new LinkedList<>();
        LinkedList<TestNode> values = new LinkedList<>();
        while (true) {
            keys.add(TestNode.parse(tokens));
            if (!tokens.tokenIs(":")) {
                throw new ParserException("Dict comprehension must have colon");
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
            throw new ParserException("Unmatched brace");
        }
        tokens.nextToken();
        return new DictLiteralNode(keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }
}
