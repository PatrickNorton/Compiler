import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

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
    private TestNode preDot;
    private NameNode[] postDots;

    /**
     * Create a new instance of DottedVariableNode.
     * @param preDot The token leading off before the first dot
     * @param postDot The tokens which come after the first dot
     */
    @Contract(pure = true)
    public DottedVariableNode(TestNode preDot, NameNode... postDot) {
        this.preDot = preDot;
        this.postDots = postDot;
    }

    /**
     * The constructor for an empty DottedVariableNode, which can be used in
     * certain situations where some code might require it.
     */
    @Contract(pure = true)
    public DottedVariableNode() {
        this.preDot = new VariableNode();
        this.postDots = new NameNode[0];
    }

    public TestNode getPreDot() {
        return preDot;
    }

    public TestNode[] getPostDots() {
        return postDots;
    }

    public boolean isEmpty() {
        return (preDot instanceof VariableNode) && ((VariableNode) preDot).isEmpty();
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
    static DottedVariableNode parse(@NotNull TokenList tokens) {  // FIXME: parse ought to be replaced with parseName
        LinkedList<VariableNode> names = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME)) {
            names.add(VariableNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            tokens.nextToken();
        }
        return new DottedVariableNode(names.removeFirst(), names.toArray(new VariableNode[0]));
    }

    /**
     * Parse a new DottedVariableNode from a list of tokens.
     * <p>
     *     This method also may or may not work, but it seems to do so better
     *     than {@link DottedVariableNode#parse} at the moment, so we'll see.
     *     Syntax for this is: <code>{@link TestNode} *("." {@link
     *     VariableNode})</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed DottedVariableNode
     */
    @NotNull
    static DottedVariableNode parseName(@NotNull TokenList tokens) {  // FIXME? Equivalent to .parse
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
        tokens.nextToken();
        LinkedList<NameNode> postDot = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME, TokenType.OPERATOR_SP)) {
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                postDot.add(SpecialOpNameNode.parse(tokens));
                break;
            }
            postDot.add(NameNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            tokens.nextToken();
        }
        return new DottedVariableNode(preDot, postDot.toArray(new NameNode[0]));
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
        if (tokens.tokenIs("(") && !tokens.braceContains("in", "for")) {
            tokens.nextToken();
            DottedVariableNode[] vars = parseList(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(DottedVariableNode.parseName(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new DottedVariableNode[0]);
    }
}
