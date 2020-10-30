package main.java.parser;

/**
 * The class representing a synchronized statement, for thread-safety.
 *
 * @author Patrick Norton
 */
public class SynchronizedStatementNode implements ComplexStatementNode {
    private LineInfo lineInfo;
    private TestNode value;
    private StatementBodyNode body;

    public SynchronizedStatementNode(LineInfo info, TestNode value, StatementBodyNode body) {
        this.lineInfo = info;
        this.value = value;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getValue() {
        return value;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    /**
     * Parse a SynchronizedStatementNode from a list of tokens.
     * <p>
     *     The syntax for a synchronized statement is: <code>"sync" {@link
     *     TestNode} {@link StatementBodyNode}</code>.
     * </p>
     *
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed node
     */

    static SynchronizedStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.SYNC);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode value = TestNode.parse(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new SynchronizedStatementNode(info, value, body);
    }

    @Override
    public String toString() {
        return String.format("sync %s %s", value, body);
    }
}
