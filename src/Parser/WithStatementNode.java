package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The class representing a context statement.
 * @author Patrick Norton
 */
public class WithStatementNode implements ComplexStatementNode {
    private TestNode[] managed;
    private VariableNode[] vars;
    private StatementBodyNode body;

    @Contract(pure = true)
    public WithStatementNode(TestNode[] managed, VariableNode[] vars, StatementBodyNode body) {
        this.managed = managed;
        this.vars = vars;
        this.body = body;
    }

    public TestNode[] getManaged() {
        return managed;
    }

    public VariableNode[] getVars() {
        return vars;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    /**
     * Parse a Parser.WithStatementNode from a list of tokens.
     * <p>
     *     The syntax for a Parser.WithStatementNode is: <code>"with" {@link TestNode}
     *     *("," {@link TestNode}) [","] "as" {@link VariableNode} *(","
     *     {@link TestNode}) [","] {@link StatementBodyNode}</code>. The list
     *     of tokens must begin with "with" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed Parser.WithStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static WithStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("with");
        tokens.nextToken();
        LinkedList<TestNode> managed = new LinkedList<>();
        while (!tokens.tokenIs("as")) {
            managed.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
            } else if (!tokens.tokenIs("as")) {
                throw new ParserException("Expected comma or as, got "+tokens.getFirst());
            }
        }
        VariableNode[] vars = VariableNode.parseList(tokens,  false);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new WithStatementNode(managed.toArray(new TestNode[0]), vars, body);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : managed) {
            sj.add(t.toString());
        }
        String managed = sj.toString();
        sj = new StringJoiner(", ");
        for (VariableNode v : vars) {
            sj.add(v.toString());
        }
        return "with " + managed + " as " + sj + " " + body;
    }
}
