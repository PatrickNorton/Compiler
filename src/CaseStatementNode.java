import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing the "case" clause in a switch statement.
 * @author Patrick Norton
 * @see SwitchStatementNode
 */
public class CaseStatementNode {
    private AtomicNode label;
    private StatementBodyNode body;
    private boolean fallthrough;

    @Contract(pure = true)
    public CaseStatementNode(AtomicNode label, StatementBodyNode body, boolean fallthrough) {
        this.label = label;
        this.body = body;
        this.fallthrough = fallthrough;
    }

    public AtomicNode getLabel() {
        return label;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public boolean hasFallthrough() {
        return fallthrough;
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
        assert tokens.tokenIs("case");
        return parse(tokens, tokens.tokenIs(tokens.sizeOfVariable(1), ":"));
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
        assert tokens.tokenIs("case");
        tokens.nextToken();
        AtomicNode label = AtomicNode.parseLabel(tokens);  // FIXME: parse list if !fallthrough
        StatementBodyNode body;
        if (fallthrough) {  // TODO: Turn into StatementBodyNode.parseSwitch
            if (!tokens.tokenIs(":")) {
                throw new ParserException("Expected :, got " + tokens.getFirst());
            }
            tokens.nextToken(true);
            LinkedList<BaseNode> statements = new LinkedList<>();
            while (!tokens.tokenIs("case")) {
                statements.add(BaseNode.parse(tokens));
            }
            body = new StatementBodyNode(statements.toArray(new BaseNode[0]));
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new CaseStatementNode(label, body, fallthrough);
    }
}
