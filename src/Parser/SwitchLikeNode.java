package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
/**
 * The abstract class representing a switch-like node.
 * <p>
 *     This class does <u>not</u> implement {@link FlowStatementNode}, because
 *     {@link FlowStatementNode} requires a getBody method, and switch
 *     statements, due to their reliance on case statements, do not implement a
 *     body.
 * </p>
 *
 * @author Patrick Norton
 */
public abstract class SwitchLikeNode implements StatementNode, EmptiableNode {
    private TestNode switched;
    private CaseStatementNode[] cases;
    private DefaultStatementNode defaultStatement;
    private boolean fallthrough;

    @Contract(pure = true)
    public SwitchLikeNode(TestNode switched, boolean fallthrough, CaseStatementNode[] cases, DefaultStatementNode defaultStatement) {
        this.switched = switched;
        this.fallthrough = fallthrough;
        this.cases = cases;
        this.defaultStatement = defaultStatement;
    }

    public TestNode getSwitched() {
        return switched;
    }

    public boolean hasFallthrough() {
        return fallthrough;
    }

    public CaseStatementNode[] getCases() {
        return cases;
    }

    public DefaultStatementNode getDefaultStatement() {
        return defaultStatement;
    }

    public boolean isEmpty() {
        return cases.length == 0 && defaultStatement.isEmpty();
    }

    /**
     * Parse a switch statement or expression from a list of tokens.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed SwitchLikeNode
     */
    @NotNull
    static SwitchLikeNode parse(TokenList tokens) {
        return parse(tokens, false, false);
    }

    /**
     * Parse a switch statement or expression from a list of tokens.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param isDetermined If the statement is determined to be an expression
     * @param isExpression If the statement is an expression
     * @return The freshly parsed SwitchLikeNode
     */
    @NotNull
    @Contract("_, _, _ -> new")
    static SwitchLikeNode parse(@NotNull TokenList tokens, boolean isDetermined, boolean isExpression) {
        assert tokens.tokenIs(Keyword.SWITCH);
        tokens.nextToken();
        TestNode switched = TestNode.parse(tokens);
        if (!tokens.tokenIs("{")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken(true);
        if (!isDetermined && tokens.tokenIs(Keyword.CASE)) {
            isExpression = tokens.lineContains(TokenType.DOUBLE_ARROW);
        }
        boolean fallthrough = false;
        LinkedList<CaseStatementNode> cases = new LinkedList<>();
        while (tokens.tokenIs(Keyword.CASE)) {
            if (isExpression) {
                cases.add(CaseStatementNode.parseExpression(tokens));
            } else if (cases.isEmpty()) {
                cases.add(CaseStatementNode.parse(tokens));
                fallthrough = cases.getLast().hasFallthrough();
            } else {
                cases.add(CaseStatementNode.parse(tokens, fallthrough));
            }
            tokens.passNewlines();
        }
        DefaultStatementNode defaultStatement;
        if (tokens.tokenIs(Keyword.DEFAULT)) {
            if (isExpression) {
                defaultStatement = DefaultStatementNode.parseExpression(tokens);
            } else if (cases.isEmpty()) {
                defaultStatement = DefaultStatementNode.parse(tokens);
                fallthrough = defaultStatement.hasFallthrough();
            } else {
                defaultStatement = DefaultStatementNode.parse(tokens, fallthrough);
            }
        } else {
            defaultStatement = new DefaultStatementNode(fallthrough);
        }
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        if (isExpression) {
            return new SwitchExpressionNode(switched, cases.toArray(new CaseStatementNode[0]), defaultStatement);
        } else {
            return new SwitchStatementNode(switched, fallthrough, cases.toArray(new CaseStatementNode[0]), defaultStatement);
        }
    }

    @Override
    public String toString() {
        return "switch " + switched + (!isEmpty() ? " {...}" : " {}");
    }
}
