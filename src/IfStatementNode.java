import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing an if-elif-else statement.
 * @author Patrick Norton
 * @see ElifStatementNode
 */
public class IfStatementNode implements FlowStatementNode {
    private TestNode conditional;
    private StatementBodyNode body;
    private ElifStatementNode[] elifs;
    private StatementBodyNode else_stmt;

    /**
     * Create a new instance of IfStatementNode.
     * @param conditional The conditional to test
     * @param body The body of the initial if-statement
     * @param elifs All elif statements post-ceding the initial if
     * @param else_stmt The else statement at the end
     */
    @Contract(pure = true)
    public IfStatementNode(TestNode conditional, StatementBodyNode body, ElifStatementNode[] elifs, StatementBodyNode else_stmt) {
        this.conditional = conditional;
        this.body = body;
        this.elifs = elifs;
        this.else_stmt = else_stmt;
    }

    public TestNode getConditional() {
        return conditional;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public ElifStatementNode[] getElifs() {
        return elifs;
    }

    public StatementBodyNode getElse_stmt() {
        return else_stmt;
    }

    /**
     * Parse an if statement from a list of tokens.
     * <p>
     *     The grammar of an if statement is: <code>"if" {@link TestNode}
     *     {@link StatementBodyNode} *("elif" {@link TestNode} {@link
     *     StatementBodyNode}) ["else" {@link StatementBodyNode}]</code>. The
     *     first node in the TokenList passed in must be "if".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly-parsed if statement
     */
    @NotNull
    @Contract("_ -> new")
    static IfStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("if");
        tokens.nextToken();
        TestNode test = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        LinkedList<ElifStatementNode> elifs = new LinkedList<>();
        while (tokens.tokenIs("elif")) {
            tokens.nextToken();
            TestNode elif_test = TestNode.parse(tokens);
            StatementBodyNode elif_body = StatementBodyNode.parse(tokens);
            elifs.add(new ElifStatementNode(elif_test, elif_body));
        }
        StatementBodyNode else_stmt = StatementBodyNode.parseOnToken(tokens, "else");
        tokens.Newline();
        return new IfStatementNode(test, body, elifs.toArray(new ElifStatementNode[0]), else_stmt);
    }

    @Override
    public String toString() {
        return "if " + conditional + " " + body;
    }
}
