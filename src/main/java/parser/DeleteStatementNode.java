package main.java.parser;

/**
 * The node representing the deletion of a variable.
 *
 * @author Patrick Norton
 */
public class DeleteStatementNode implements SimpleStatementNode {
    private LineInfo lineInfo;
    private TestNode deleted;

    public DeleteStatementNode(LineInfo lineInfo, TestNode deleted) {
        this.lineInfo = lineInfo;
        this.deleted = deleted;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getDeleted() {
        return deleted;
    }

    /**
     * Parse DeleteStatementNode from a list of tokens.
     * <p>
     *     The syntax of DeleteStatementNode is as follows: <code>"del"
     *     {@link TestNode}</code>.
     * </p>
     * @param tokens The list of tokens which is to be parsed destructively
     * @return The newly parsed DeleteStatementNode
     */

    static DeleteStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.DEL);
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        TestNode deletion = TestNode.parse(tokens);
        return new DeleteStatementNode(lineInfo, deletion);
    }

    @Override
    public String toString() {
        return "del " + deleted;
    }
}
