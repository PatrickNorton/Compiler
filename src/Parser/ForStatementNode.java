package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a for-statement.
 * @author Patrick Norton
 */
public class ForStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private VarLikeNode[] vars;
    private TestListNode iterables;
    private StatementBodyNode body;
    private StatementBodyNode nobreak;

    /**
     * Construct a new instance of ForStatementNode.
     * @param vars The variables iterating on each loop
     * @param iterables The iterables being iterated over
     * @param body The body of the loop
     * @param nobreak The nobreak statement body
     */
    @Contract(pure = true)
    public ForStatementNode(LineInfo lineInfo, VarLikeNode[] vars, TestListNode iterables,
                            StatementBodyNode body, StatementBodyNode nobreak) {
        this.lineInfo = lineInfo;
        this.vars = vars;
        this.iterables = iterables;
        this.body = body;
        this.nobreak = nobreak;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public VarLikeNode[] getVars() {
        return vars;
    }

    public TestListNode getIterables() {
        return iterables;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getNobreak() {
        return nobreak;
    }

    /**
     * Parse a ForStatementNode from a list of tokens.
     *
     * <p>
     *     The syntax of a for loop is: <code>"for" {@link VarLikeNode} *(","
     *     {@link VarLikeNode}) [","] "in" {@link TestNode} *("," {@link
     *     TestNode}) [","] {@link StatementBodyNode} ["nobreak" {@link
     *     StatementBodyNode}]</code>. The list of tokens must begin with a
     *     "for" in order to parse properly.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed ForStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ForStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.FOR);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        boolean ignoreNewlines = tokens.tokenIs("(");
        if (ignoreNewlines) {
            tokens.nextToken(true);
        }
        VarLikeNode[] vars = VarLikeNode.parseList(tokens, ignoreNewlines);
        if (!tokens.tokenIs(Keyword.IN)) {
            throw tokens.error("Expected in, got " + tokens.getFirst());
        }
        tokens.nextToken(ignoreNewlines);
        TestListNode iterables = TestListNode.parse(tokens, ignoreNewlines);
        if (ignoreNewlines) {
            if (tokens.tokenIs(")")) {
                tokens.nextToken();
            } else {
                throw tokens.error("Unexpected " + tokens.getFirst());
            }
        }
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode nobreak = StatementBodyNode.parseOnToken(tokens, "nobreak");
        return new ForStatementNode(lineInfo, vars, iterables, body, nobreak);
    }

    @Override
    public String toString() {
        String vars = TestNode.toString(this.vars);
        return String.format("for %s in %s %s", vars, iterables, body);
    }
}
