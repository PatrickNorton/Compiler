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
    private TypeNode[] excepted;
    private VariableNode as_var;
    private StatementBodyNode else_stmt;
    private StatementBodyNode finally_stmt;

    @Contract(pure = true)
    public TryStatementNode(LineInfo lineInfo, StatementBodyNode body, StatementBodyNode except, TypeNode[] excepted,
                            VariableNode as_var, StatementBodyNode else_stmt, StatementBodyNode finally_stmt) {
        this.lineInfo = lineInfo;
        this.body = body;
        this.except = except;
        this.excepted = excepted;
        this.as_var = as_var;
        this.else_stmt = else_stmt;
        this.finally_stmt = finally_stmt;
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

    public TypeNode[] getExcepted() {
        return excepted;
    }

    public VariableNode getAs_var() {
        return as_var;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }

    public StatementBodyNode getFinally_stmt() {
        return finally_stmt;
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
        StatementBodyNode except = new StatementBodyNode();
        TypeNode[] excepted = new TypeNode[0];
        VariableNode as = VariableNode.empty();
        StatementBodyNode elseStmt = new StatementBodyNode();
        if (tokens.tokenIs(Keyword.EXCEPT)) {
            tokens.nextToken();
            excepted = TypeNode.parseList(tokens);
            as = VariableNode.parseOnToken(tokens, Keyword.AS);
            except = StatementBodyNode.parse(tokens);
            elseStmt = StatementBodyNode.parseOnToken(tokens, Keyword.ELSE);
        }
        StatementBodyNode finallyStmt = StatementBodyNode.parseOnToken(tokens, Keyword.FINALLY);
        if (except.isEmpty() && finallyStmt.isEmpty()) {
            throw tokens.error("Try statement must either have an except or finally clause");
        }
        return new TryStatementNode(info, body, except, excepted, as, elseStmt, finallyStmt);
    }

    @Override
    public String toString() {
        if (!except.isEmpty()) {
            String excepted = TestNode.toString(this.excepted);
            return String.format("try %s except %s%s", body, excepted, (as_var.isEmpty() ? "" : " as " + as_var));
        } else if (!finally_stmt.isEmpty()) {
            return String.format("try %s finally %s", body, finally_stmt);
        } else {
            return "try " + body;
        }
    }
}
