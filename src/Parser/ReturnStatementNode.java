package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a return statement.
 *
 * @author Patrick Norton
 */
public class ReturnStatementNode implements SimpleFlowNode {
    private LineInfo lineInfo;
    private TestNode[] returned;
    private TestNode cond;

    /**
     * Construct a new instance of ReturnStatementNode.
     * @param returned The list of tokens that are returned
     * @param cond The condition as to whether or not there is a return
     */
    @Contract(pure = true)
    public ReturnStatementNode(LineInfo lineInfo, TestNode[] returned, TestNode cond) {
        this.lineInfo = lineInfo;
        this.returned = returned;
        this.cond = cond;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode[] getReturned() {
        return returned;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    /**
     * Parse a new return statement from a list of tokens.
     * <p>
     *     The syntax for a return statement is: <code>"return" [{@link
     *     TestNode} *("," {@link TestNode}) [","]] ["if" {@link
     *     TestNode}]</code>. The list of tokens must begin with "return".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed ReturnStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ReturnStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.RETURN);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        boolean is_conditional = tokens.lineContains(Keyword.IF) && !tokens.lineContains(Keyword.ELSE);
        TestNode[] returned;
        if (is_conditional && !tokens.tokenIs(Keyword.IF)) {
            returned = parseReturns(tokens);
            if (!tokens.tokenIs(Keyword.IF)) {
                throw tokens.error("Unexpected " + tokens.getFirst());
            }
        } else if (!tokens.tokenIs(TokenType.NEWLINE)) {
            returned = TestNode.parseListDanglingIf(tokens, false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = TestNode.empty();
        if (is_conditional) {
            assert tokens.tokenIs(Keyword.IF);
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        return new ReturnStatementNode(lineInfo, returned, cond);
    }

    /**
     * Parse the returned values in a conditional return statement.
     * @param tokens The list of tokens to be destructively parsed
     * @return The values to be returned
     */
    @NotNull
    private static TestNode[] parseReturns(@NotNull TokenList tokens) {
        assert !tokens.tokenIs(Keyword.IF);
        LinkedList<TestNode> returned_list = new LinkedList<>();
        do {
            returned_list.add(TestNode.parseNoTernary(tokens, false));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken();
        } while (!tokens.tokenIs(Keyword.IF));
        return returned_list.toArray(new TestNode[0]);
    }

    @Override
    public String toString() {
        return "return " + TestNode.toString(returned) + (!cond.isEmpty() ? " if " + cond : "");
    }
}
