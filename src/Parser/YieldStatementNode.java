package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The class representing a yield statement.
 * @author Patrick Norton
 */
public class YieldStatementNode implements SimpleStatementNode {
    private boolean is_from;
    private TestNode[] yielded;

    @Contract(pure = true)
    public YieldStatementNode(boolean is_from, TestNode... yielded) {
        this.is_from = is_from;
        this.yielded = yielded;
    }

    public TestNode[] getYielded() {
        return yielded;
    }

    public boolean getIs_from() {
        return is_from;
    }

    /**
     * Given a list of tokens, parse a Parser.YieldStatementNode.
     * <p>
     *     The syntax for a yield statement is: <code>"yield" ["from"] {@link
     *     TestNode} *("," {@link TestNode}) [",']</code>. The passed list must
     *     begin with "yield" when passed.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed Parser.YieldStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static YieldStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("yield");
        tokens.nextToken();
        boolean is_from = tokens.tokenIs("from");
        if (is_from) {
            tokens.nextToken();
        }
        LinkedList<TestNode> yields = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE)) {
            yields.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
                continue;
            }
            if (!tokens.tokenIs(TokenType.NEWLINE)) {
                throw new ParserException("Comma must separate yields");
            }
        }
        return new YieldStatementNode(is_from, yields.toArray(new TestNode[0]));
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : yielded) {
            sj.add(t.toString());
        }
        return (is_from ? "yield from " : "yield ") + sj;
    }
}
