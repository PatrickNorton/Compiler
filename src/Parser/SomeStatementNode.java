package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a some statement.
 * @author Patrick Norton
 */
public class SomeStatementNode implements SubTestNode {
    private LineInfo lineInfo;
    private TestNode contained;
    private TestNode container;

    /**
     * Create a new instance of SomeStatementNode.
     * @param contained The node which is contained
     * @param container The node which being tested if contained is a member
     */
    @Contract(pure = true)
    public SomeStatementNode(LineInfo lineInfo, TestNode contained, TestNode container) {
        this.lineInfo = lineInfo;
        this.contained = contained;
        this.container = container;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getContained() {
        return contained;
    }

    public TestNode getContainer() {
        return container;
    }

    /**
     * Parse a SomeStatementNode from a list of tokens.
     * <p>
     *     The syntax for a some statement is: <code>"some" {@link TestNode}
     *     "in" {@link TestNode}</code>. The list of tokens must begin with
     *     "some" when passed.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed SomeStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static SomeStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.SOME);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        TestNode contained = TestNode.parse(tokens);
        if (!(contained instanceof OperatorNode)) {
            throw tokens.errorExpected("in");
        }
        OperatorNode inStmt = (OperatorNode) contained;
        if (inStmt.getOperator() != OperatorTypeNode.IN) {
            throw tokens.errorExpected("in");
        }
        ArgumentNode[] operands = inStmt.getOperands();
        return new SomeStatementNode(lineInfo, operands[0].getArgument(), operands[1].getArgument());
    }

    @Override
    public String toString() {
        return "some " + contained + " in " + container;
    }
}
