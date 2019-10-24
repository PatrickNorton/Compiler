package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
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
        List<TestNode> nodes = new LinkedList<TestNode>();
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
     * Parse a new DottedVariableNode from a list of tokens.
     * <p>
     *     This method may or may not be depreciated, <b>do not use</b> until
     *     I've actually figured out which one will parse things correctly
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DottedVariableNode
     */
    @NotNull
    @Contract("_ -> new")
    static DottedVariableNode parseNamesOnly(@NotNull TokenList tokens) {
        NameNode name = NameNode.parse(tokens);
        DottedVar[] names = DottedVar.parseAll(tokens);
        return new DottedVariableNode(name, names);
    }

    /**
     * Parse a new DottedVariableNode from a list of tokens.
     * <p>
     *     This method also may or may not work, but it seems to do so better
     *     than {@link DottedVariableNode#parseNamesOnly} at the moment, so we'll see.
     *     Syntax for this is: <code>{@link TestNode} *("." {@link
     *     VariableNode})</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed DottedVariableNode
     */
    @NotNull
    static DottedVariableNode parseName(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = NameNode.parse(tokens);
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, name);
        } else {
            return new DottedVariableNode(name);
        }
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
        assert tokens.tokenIs(TokenType.DOT);
        DottedVar[] postDots = DottedVar.parseAll(tokens);
        return new DottedVariableNode(preDot, postDots);
    }

    /**
     * Given a list of tokens, parses out a list of dotted variables.
     * <p>
     *     The syntax for this is: <code>{@link DottedVariableNode} *(","
     *     {@link DottedVariableNode} [","]</code>. Newlines may or may not be
     *     ignored.
     * </p>
     * @param tokens The list of tokens to parse
     * @param ignore_newlines Whether or not newlines should be ignored
     * @return The freshly parsed array of DottedVariableNodes
     */
    static DottedVariableNode[] parseList(TokenList tokens, boolean ignore_newlines) {
        LinkedList<DottedVariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            tokens.passNewlines();
        }
        if (tokens.tokenIs("(") && !tokens.braceContains(Keyword.IN, Keyword.FOR)) {
            tokens.nextToken();
            DottedVariableNode[] vars = parseList(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw tokens.error("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw tokens.error("Unmatched braces");
            }
            variables.add(parseName(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new DottedVariableNode[0]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(preDot);
        for (DottedVar d : newPostDots) {
            sb.append(d);
        }
        return sb.toString();
    }
}
