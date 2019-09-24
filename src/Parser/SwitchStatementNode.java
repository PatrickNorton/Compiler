package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a switch statement.
 * <p>
 *     This class does <u>not</u> implement FlowStatementNode, because
 *     FlowStatementNode requires a getBody method, and switch statements,
 *     due to their reliance on case statements, do not implement a body.
 * </p>
 * @author Patrick Norton
 * @see CaseStatementNode
 */
public class SwitchStatementNode implements StatementNode, EmptiableNode {
    private TestNode switched;
    private CaseStatementNode[] cases;
    private DefaultStatementNode defaultStatement;
    private boolean fallthrough;

    @Contract(pure = true)
    public SwitchStatementNode(TestNode switched, boolean fallthrough, CaseStatementNode[] cases, DefaultStatementNode defaultStatement) {
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

    @Override
    public boolean isEmpty() {
        return cases.length == 0 && defaultStatement.isEmpty();
    }

    /**
     * Parse a switch statement from a list of tokens.
     * <p>
     *     The syntax for a switch statement is: <code>"switch" "{" *{@link
     *     CaseStatementNode} [{@link DefaultStatementNode}] "}"</code>. The
     *     list of tokens passed must begin with "switch", and all case
     *     statements must be of the same fallthrough type.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed Parser.SwitchStatementNode.
     */
    @NotNull
    @Contract("_ -> new")
    public static SwitchStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("switch");
        tokens.nextToken();
        TestNode switched = TestNode.parse(tokens);
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Expected {, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        boolean fallthrough = false;
        LinkedList<CaseStatementNode> cases = new LinkedList<>();
        while (tokens.tokenIs("case")) {
            if (cases.isEmpty()) {
                cases.add(CaseStatementNode.parse(tokens));
                fallthrough = cases.getLast().hasFallthrough();
            } else {
                cases.add(CaseStatementNode.parse(tokens, fallthrough));
            }
            tokens.passNewlines();
        }
        DefaultStatementNode defaultStatement;
        if (tokens.tokenIs("default")) {
            if (cases.isEmpty()) {
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
            throw new ParserException("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        return new SwitchStatementNode(switched, fallthrough, cases.toArray(new CaseStatementNode[0]), defaultStatement);
    }

    @Override
    public String toString() {
        return "switch " + switched + (!isEmpty() ? " {...}" : " {}");
    }

}
