package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing the "case" clause in a switch statement.
 * @author Patrick Norton
 * @see SwitchStatementNode
 */
public class CaseStatementNode implements BaseNode, EmptiableNode {
    private LineInfo lineInfo;
    private TestNode[] label;
    private StatementBodyNode body;
    private boolean arrow;

    @Contract(pure = true)
    public CaseStatementNode(LineInfo lineInfo, TestNode[] label, StatementBodyNode body, boolean arrow) {
        this.lineInfo = lineInfo;
        this.label = label;
        this.body = body;
        this.arrow = arrow;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode[] getLabel() {
        return label;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public boolean isArrow() {
        return arrow;
    }

    @Override
    public boolean isEmpty() {
        return body.isEmpty();
    }

    /**
     * Parse a case statement from a list of tokens.
     * <p>
     *     The syntax for a case statement is: <code>"case" {@link TestNode}
     *     *("," {@link TestNode}) [","] (("=>" {@link TestNode}) | {@link
     *     StatementBodyNode})</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed CaseStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    public static CaseStatementNode parse(@NotNull TokenList tokens) {
        if (tokens.tokenIs(Keyword.DEFAULT)) {
            return DefaultStatementNode.parse(tokens);
        }
        assert tokens.tokenIs(Keyword.CASE);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode[] label = TestNode.parseList(tokens, false);
        StatementBodyNode body;
        boolean arrow = tokens.tokenIs(TokenType.DOUBLE_ARROW);
        if (arrow) {
            LineInfo arrowInfo = tokens.lineInfo();
            tokens.nextToken(true);
            body = new StatementBodyNode(arrowInfo, TestNode.parse(tokens, false));
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new CaseStatementNode(info, label, body, arrow);
    }

    @Override
    public String toString() {
        return String.format("case %s %s", TestNode.toString(label), arrow ? "=> ..." : body);
    }
}
