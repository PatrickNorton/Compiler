package main.java.parser;

import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
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

    public Iterable<Pair<TestNode, TestNode>> pairs() {
        return PairIterator::new;
    }

    class PairIterator implements Iterator<Pair<TestNode, TestNode>> {
        int number = 0;

        @Override
        public boolean hasNext() {
            return number < size();
        }

        @Override
        public Pair<TestNode, TestNode> next() {
            Pair<TestNode, TestNode> pair = Pair.of(keys[number], values[number]);
            number++;
            return pair;
        }
    }

    public int size() {
        return keys.length;
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
            tokens.expect("}");
            return new DictLiteralNode(info);
        }
        do {
            if (tokens.tokenIs("**")) {
                tokens.nextToken(true);
                keys.add(TestNode.empty());
            } else {
                keys.add(TestNode.parse(tokens, true));
                if (!tokens.tokenIs(":")) {
                    throw tokens.error("Dict comprehension must have colon");
                }
                tokens.nextToken(true);
            }
            values.add(TestNode.parse(tokens, true));
            if (!tokens.tokenIs(",")) {
                if (!tokens.tokenIs("}")) {
                    throw tokens.error("Unmatched brace");
                } else {
                    break;
                }
            }
            tokens.nextToken(true);
        } while (!tokens.tokenIs("}"));
        tokens.nextToken();
        return new DictLiteralNode(info, keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }

    @Override
    public String toString() {
        switch (keys.length) {
            case 0:
                return "{:}";
            case 1:
                return keys[0].isEmpty() ? "{**" + values[0] + "}" : String.format("{%s: %s}", keys[0], values[0]);
            default:
                return keys[0].isEmpty() ? "{**" + values[0] + "...}" : String.format("{%s: %s, ...}", keys[0], values[0]);
        }
    }
}
