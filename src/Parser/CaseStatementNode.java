package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

/**
 * The class representing the "case" clause in a switch statement.
 * @author Patrick Norton
 * @see SwitchStatementNode
 */
public class CaseStatementNode implements BaseNode, EmptiableNode {
    private AtomicNode[] label;
    private StatementBodyNode body;
    private boolean fallthrough;

    @Contract(pure = true)
    public CaseStatementNode(AtomicNode[] label, StatementBodyNode body, boolean fallthrough) {
        this.label = label;
        this.body = body;
        this.fallthrough = fallthrough;
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
        tokens.nextToken();
        AtomicNode[] label = fallthrough ? new AtomicNode[]{AtomicNode.parseLabel(tokens)} : AtomicNode.parseLabelList(tokens);
        StatementBodyNode body;
        if (fallthrough) {
            if (!tokens.tokenIs(":")) {
                throw new ParserException("Expected :, got " + tokens.getFirst());
            }
            tokens.nextToken(true);
            body = StatementBodyNode.parseSwitch(tokens);
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new CaseStatementNode(label, body, fallthrough);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (AtomicNode i : label) {
            joiner.add(i.toString());
        }
        return "case " + joiner + (fallthrough ? ": ..." : "{...}");
    }
}
