import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

/**
 * The class representing a return statement.
 * @author Patrick Norton
 */
public class ReturnStatementNode implements SimpleFlowNode {
    private TestNode[] returned;
    private TestNode cond;

    /**
     * Construct a new instance of ReturnStatementNode.
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
     * @return The newly parsed ReturnStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ReturnStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("return");
        tokens.nextToken();
        boolean is_conditional = false;
        if (tokens.tokenIs("(") && tokens.getToken(tokens.sizeOfBrace(0) + 1).is("if")
                && !tokens.lineContains("else")) {
            tokens.nextToken();
            is_conditional = true;
        }
        TestNode[] returned;
        if (!tokens.tokenIs(TokenType.NEWLINE) && !tokens.tokenIs("if")) {
            returned = TestNode.parseList(tokens, false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = null;
        if (is_conditional) {
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Expected ), got " + tokens.getFirst());
            }
            cond = TestNode.parse(tokens);
        }
        tokens.Newline();
        return new ReturnStatementNode(returned, cond);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : returned) {
            sj.add(t.toString());
        }
        return "return " + sj + (cond != null ? "if " + cond : "");
    }
}
