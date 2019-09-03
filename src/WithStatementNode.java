import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

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
        tokens.Newline();
        return new WithStatementNode(managed.toArray(new TestNode[0]), vars, body);
    }
}
