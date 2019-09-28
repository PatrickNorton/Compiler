package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The class representing a return statement.
 * @author Patrick Norton
 */
public class ReturnStatementNode implements SimpleFlowNode {
    private TestNode[] returned;
    private TestNode cond;

    /**
     * Construct a new instance of Parser.ReturnStatementNode.
     * @param returned The list of tokens that are returned
     * @param cond The condition as to whether or not there is a return
     */
    @Contract(pure = true)
    public ReturnStatementNode(TestNode[] returned, TestNode cond) {
        this.returned = returned;
        this.cond = cond;
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
     * @return The newly parsed Parser.ReturnStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ReturnStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.RETURN);
        tokens.nextToken();
        boolean is_conditional = tokens.lineContains(Keyword.IF) && !tokens.lineContains(Keyword.ELSE);
        TestNode[] returned;
        if (is_conditional && !tokens.tokenIs(Keyword.IF)) {
            returned = parseReturns(tokens);
            if (!tokens.tokenIs(Keyword.IF)) {
                throw new ParserException("Unexpected " + tokens.getFirst());
            }
        } else if (!tokens.tokenIs(TokenType.NEWLINE)) {
            returned = TestNode.parseList(tokens, false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = TestNode.empty();
        if (is_conditional) {
            assert tokens.tokenIs(Keyword.IF);
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        return new ReturnStatementNode(returned, cond);
    }

    @NotNull
    private static TestNode[] parseReturns(@NotNull TokenList tokens) {
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
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : returned) {
            sj.add(t.toString());
        }
        return "return " + sj + (!cond.isEmpty() ? "if " + cond : "");
    }
}
