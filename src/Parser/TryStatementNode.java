package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing the try-except-finally statement.
 * <p>
 *     This is a relatively complex class data-wise, as it has a lot of
 *     component statements.
 * </p>
 * @author Patrick Norton
 */
public class TryStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private StatementBodyNode body;
    private StatementBodyNode except;
    private TypeLikeNode[] excepted;
    private VariableNode asVar;
    private StatementBodyNode elseStmt;
    private StatementBodyNode finallyStmt;

    @Contract(pure = true)
    public TryStatementNode(LineInfo lineInfo, StatementBodyNode body, StatementBodyNode except, TypeLikeNode[] excepted,
                            VariableNode asVar, StatementBodyNode elseStmt, StatementBodyNode finallyStmt) {
        this.lineInfo = lineInfo;
        this.body = body;
        this.except = except;
        this.excepted = excepted;
        this.asVar = asVar;
        this.elseStmt = elseStmt;
        this.finallyStmt = finallyStmt;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public StatementBodyNode getExcept() {
        return except;
    }

    public TypeLikeNode[] getExcepted() {
        return excepted;
    }

    public VariableNode getAsVar() {
        return asVar;
    }

    public StatementBodyNode getElseStmt() {
        return elseStmt;
    }

    public StatementBodyNode getFinallyStmt() {
        return finallyStmt;
    }

    /**
     * Parse a try statement from a list of tokens.
     * <p>
     *     The syntax for a try statement is: <code>"try" {@link
     *     StatementBodyNode} ["except" {@link VariableNode} [[","] {@link
     *     VariableNode}] [","] ["as" {@link VariableNode} ["else" {@link
     *     StatementBodyNode}]] ["finally" {@link StatementBodyNode}]</code>.
     *     The list of tokens passed must begin with a "try", and there must be
     *     either an "except" or a "finally" in the statement.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TryStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static TryStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.TRY);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        StatementBodyNode except = StatementBodyNode.empty();
        TypeLikeNode[] excepted = new TypeNode[0];
        VariableNode as = VariableNode.empty();
        StatementBodyNode elseStmt = StatementBodyNode.empty();
        if (tokens.tokenIs(Keyword.EXCEPT)) {
            tokens.nextToken();
            excepted = TypeLikeNode.parseList(tokens);
            as = VariableNode.parseOnToken(tokens, Keyword.AS);
            except = StatementBodyNode.parse(tokens);
            elseStmt = StatementBodyNode.parseOnToken(tokens, Keyword.ELSE);
        }
        StatementBodyNode finallyStmt = StatementBodyNode.parseOnToken(tokens, Keyword.FINALLY);
        if (except.isEmpty() && excepted.length == 0 && finallyStmt.isEmpty()) {
            throw tokens.error("Try statement must either have an except or finally clause");
        }
        return new TryStatementNode(info, body, except, excepted, as, elseStmt, finallyStmt);
    }

    @Override
    public String toString() {
        if (!except.isEmpty()) {
            String excepted = TestNode.toString(this.excepted);
            return String.format("try %s except %s%s", body, excepted, (asVar.isEmpty() ? "" : " as " + asVar));
        } else if (!finallyStmt.isEmpty()) {
            return String.format("try %s finally %s", body, finallyStmt);
        } else {
            return "try " + body;
        }
    }
}
