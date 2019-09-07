import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a switch statement.
 * @author Patrick Norton
 * @see CaseStatementNode
 */
public class SwitchStatementNode implements StatementNode {
    private TestNode switched;
    private CaseStatementNode[] cases;

    @Contract(pure = true)
    public SwitchStatementNode(TestNode switched, CaseStatementNode... cases) {
        this.switched = switched;
        this.cases = cases;
    }

    public TestNode getSwitched() {
        return switched;
    }

    public CaseStatementNode[] getCases() {
        return cases;
    }

    /**
     * Parse a switch statement from a list of tokens.
     * <p>
     *     The syntax for a switch statement is: <code>"switch" "{" *{@link
     *     CaseStatementNode} "}"</code>. The list of tokens passed must begin
     *     with "switch", and all case statements must be of the same
     *     fallthrough type.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed SwitchStatementNode.
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
        }
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        return new SwitchStatementNode(switched, cases.toArray(new CaseStatementNode[0]));
    }
}
