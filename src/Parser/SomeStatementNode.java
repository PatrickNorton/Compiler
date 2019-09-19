package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a some statement.
 * @author Patrick Norton
 */
public class SomeStatementNode implements SubTestNode {
    private TestNode contained;
    private TestNode container;

    /**
     * Create a new instance of Parser.SomeStatementNode.
     * @param contained The node which is contained
     * @param container The node which being tested if contained is a member
     */
    @Contract(pure = true)
    public SomeStatementNode(TestNode contained, TestNode container) {
        this.contained = contained;
        this.container = container;
    }

    public TestNode getContained() {
        return contained;
    }

    public TestNode getContainer() {
        return container;
    }

    /**
     * Parse a Parser.SomeStatementNode from a list of tokens.
     * <p>
     *     The syntax for a some statement is: <code>"some" {@link TestNode}
     *     "in" {@link TestNode}</code>. The list of tokens must begin with
     *     "some" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed Parser.SomeStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static SomeStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("some");
        tokens.nextToken();
        TestNode contained = TestNode.parse(tokens);
        if (!(contained instanceof OperatorNode)) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
        }
        OperatorNode in_stmt = (OperatorNode) contained;
        if (in_stmt.getOperator() != OperatorTypeNode.IN) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
        }
        ArgumentNode[] operands = in_stmt.getOperands();
        return new SomeStatementNode(operands[0].getArgument(), operands[1].getArgument());
    }

    @Override
    public String toString() {
        return "some " + contained + " in " + container;
    }
}
