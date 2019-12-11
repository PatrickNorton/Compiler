package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The class representing a dotted variable.
 * <p>
 *     This class represents any time there is or could be a variable containing
 *     dots, such as {@code foo.bar.baz}. Not to be confused with its
 *     sub-component cousin, {@link VariableNode}.
 * </p>
 * @author Patrick Norton
 * @see NameNode
 * @see VariableNode
 */
public class DottedVariableNode implements NameNode {
    private LineInfo lineInfo;
    private TestNode preDot;
    private DottedVar[] newPostDots;

    public DottedVariableNode(@NotNull TestNode preDot, DottedVar... postDots) {
        this.lineInfo = preDot.getLineInfo();
        this.preDot = preDot;
        this.newPostDots = postDots;
    }

    /**
     * The constructor for an empty DottedVariableNode, which can be used in
     * certain situations where some code might require it.
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static DottedVariableNode empty() {
        return new DottedVariableNode(TestNode.empty());
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getPreDot() {
        return preDot;
    }

    public TestNode[] getPostDots() {
        List<TestNode> nodes = new ArrayList<>();
        for (DottedVar d : newPostDots) {
            nodes.add(d.getPostDot());
        }
        return nodes.toArray(new TestNode[0]);
    }

    @Override
    public boolean isEmpty() {
        return preDot.isEmpty();
    }

    /**
     * Parse a new DottedVariableNode from a list of tokens, which can only can
     * consist of names, not indices or fn-calls.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DottedVariableNode
     */
    @NotNull
    @Contract("_ -> new")
    static DottedVariableNode parseNamesOnly(@NotNull TokenList tokens) {
        return parseNamesOnly(tokens, false);
    }

    @NotNull
    @Contract("_, _ -> new")
    static DottedVariableNode parseNamesOnly(@NotNull TokenList tokens, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = VariableNode.parse(tokens);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        List<DottedVar> postDots = new ArrayList<>();
        while (tokens.tokenIs(TokenType.DOT)) {
            postDots.add(DottedVar.parse(tokens, true, ignoreNewlines));
        }
        return new DottedVariableNode(name, postDots.toArray(new DottedVar[0]));
    }

    static DottedVariableNode parseOnName(@NotNull TokenList tokens) {
        return tokens.tokenIs(TokenType.NAME) ? parseNamesOnly(tokens) : empty();
    }

    static TestNode parsePostDots(@NotNull TokenList tokens, TestNode preDot, boolean ignoreNewlines) {
        return tokens.tokenIs(TokenType.DOT)
                ? DottedVariableNode.fromExpr(tokens, preDot, ignoreNewlines)
                : preDot;
    }

    /**
     * Given a variable, parses the rest of the dotted vars after it.
     * <p>
     *     The syntax for what may be parsed here is: <code>"." {@link
     *     VariableNode} *("." {@link VariableNode})</code>. The first token in
     *     the list must be a dot.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @param preDot The node which comes before the dot
     * @return The freshly parsed node
     */
    @NotNull
    @Contract("_, _ -> new")
    static DottedVariableNode fromExpr(@NotNull TokenList tokens, TestNode preDot) {
        return fromExpr(tokens, preDot, false);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    static DottedVariableNode fromExpr(@NotNull TokenList tokens, TestNode preDot, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.DOT);
        DottedVar[] postDots = DottedVar.parseAll(tokens, ignoreNewlines);
        return new DottedVariableNode(preDot, postDots);
    }

    /**
     * Given a list of tokens, parses out a list of dotted variables, which are
     * name-only.
     * <p>
     *     The syntax for this is: <code>{@link DottedVariableNode} *(","
     *     {@link DottedVariableNode} [","]</code>. Newlines may or may not be
     *     ignored.
     * </p>
     * @param tokens The list of tokens to parse
     * @return The freshly parsed array of DottedVariableNodes
     */
    @NotNull
    static DottedVariableNode[] parseNameOnlyList(@NotNull TokenList tokens) {
        boolean isBraced;
        List<DottedVariableNode> variables = new ArrayList<>();
        if (tokens.tokenIs("(") && !tokens.braceContains(Keyword.IN, Keyword.FOR)) {
            tokens.nextToken(true);
            isBraced = true;
        } else {
            isBraced = false;
        }
        while (tokens.tokenIs(TokenType.NAME)) {
            variables.add(parseNamesOnly(tokens, isBraced));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(isBraced);
            } else {
                break;
            }
        }
        if (isBraced) {
            if (!tokens.tokenIs(")")) {
                throw tokens.error("Unmatched braces");
            }
            tokens.nextToken();
        }
        return variables.toArray(new DottedVariableNode[0]);
    }

    @Override
    public String toString() {
        if (isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        if (preDot instanceof PostDottableNode) {
            sb.append(preDot);
        } else {
            sb.append('(').append(preDot).append(')');
        }
        for (DottedVar d : newPostDots) {
            sb.append(d);
        }
        return sb.toString();
    }
}
