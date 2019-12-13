package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a context statement.
 * @author Patrick Norton
 */
public class WithStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private TestListNode managed;
    private TypedVariableNode[] vars;
    private StatementBodyNode body;

    @Contract(pure = true)
    public WithStatementNode(LineInfo lineInfo, TestListNode managed, TypedVariableNode[] vars, StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.managed = managed;
        this.vars = vars;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestListNode getManaged() {
        return managed;
    }

    public TypedVariableNode[] getVars() {
        return vars;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    /**
     * Parse a WithStatementNode from a list of tokens.
     * <p>
     *     The syntax for a WithStatementNode is: <code>"with" {@link TestNode}
     *     *("," {@link TestNode}) [","] "as" {@link VariableNode} *(","
     *     {@link TestNode}) [","] {@link StatementBodyNode}</code>. The list
     *     of tokens must begin with "with" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed WithStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static WithStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.WITH);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        TestListNode managed = TestListNode.parse(tokens, false);
        TypedVariableNode[] vars = TypedVariableNode.parseListOnToken(tokens, Keyword.AS);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new WithStatementNode(lineInfo, managed, vars, body);
    }

    @Override
    public String toString() {
        return String.format("with %s as %s %s", managed, TestNode.toString(vars), body);
    }
}
