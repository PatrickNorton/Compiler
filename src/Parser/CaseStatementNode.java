package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing the "case" clause in a switch statement.
 * @author Patrick Norton
 * @see SwitchStatementNode
 */
public class CaseStatementNode implements BaseNode, EmptiableNode {
    private LineInfo lineInfo;
    private AtomicNode[] label;
    private StatementBodyNode body;
    private boolean fallthrough;

    @Contract(pure = true)
    public CaseStatementNode(LineInfo lineInfo, AtomicNode[] label, StatementBodyNode body, boolean fallthrough) {
        this.lineInfo = lineInfo;
        this.label = label;
        this.body = body;
        this.fallthrough = fallthrough;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public AtomicNode[] getLabel() {
        return label;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public boolean hasFallthrough() {
        return fallthrough;
    }

    @Override
    public boolean isEmpty() {
        return body.isEmpty();
    }

    /**
     * Parse a case statement from a list of tokens.
     * <p>
     *     This is for case statements with unknown fallthrough. It will not
     *     error on either type of statement. For grammar, see the other parse
     *     method. The list of tokens must begin with "case" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed CaseStatementNode.
     */
    @NotNull
    @Contract("_ -> new")
    public static CaseStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CASE);
        if (tokens.tokenIs(1, TokenType.NAME)) {
            return parse(tokens, tokens.tokenIs(tokens.sizeOfVariable(1), ":"));
        } else {
            return parse(tokens, tokens.tokenIs(2, ":"));
        }
    }

    /**
     * Parse a case statement from a list of tokens.
     * <p>
     *     The syntax for a no-fallthrough case statement is: <code>"case"
     *     {@link AtomicNode} *("," {@link AtomicNode}) [","] {@link
     *     StatementBodyNode}</code>. The syntax for a with-fallthrough case
     *     statement is: <code>"case" {@link AtomicNode} ":" [NEWLINE]
     *     *({@link BaseNode} NEWLINE)</code>. The list of tokens must begin
     *     with "case" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param fallthrough Whether or not the statement allows fallthrough
     * @return The freshly parsed CaseStatementNode
     */
    @NotNull
    @Contract("_, _ -> new")
    public static CaseStatementNode parse(@NotNull TokenList tokens, boolean fallthrough) {
        assert tokens.tokenIs(Keyword.CASE);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        AtomicNode[] label = fallthrough ? new AtomicNode[]{AtomicNode.parseLabel(tokens)} : AtomicNode.parseLabelList(tokens);
        StatementBodyNode body;
        if (fallthrough) {
            if (!tokens.tokenIs(":")) {
                throw tokens.error("Expected :, got " + tokens.getFirst());
            }
            tokens.nextToken(true);
            body = StatementBodyNode.parseCase(tokens);
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new CaseStatementNode(info, label, body, fallthrough);
    }

    @NotNull
    @Contract("_ -> new")
    public static CaseStatementNode parseExpression(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CASE);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        AtomicNode[] label = AtomicNode.parseLabelList(tokens);
        if (!tokens.tokenIs(TokenType.DOUBLE_ARROW)) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        TestNode[] body = TestNode.parseList(tokens, false);
        return new CaseStatementNode(info, label, new StatementBodyNode(body), false);
    }

    @Override
    public String toString() {
        String labels = TestNode.toString(label);
        return "case " + labels + (fallthrough ? ": ..." : "{...}");
    }
}
