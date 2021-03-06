package main.java.parser;

import org.jetbrains.annotations.Contract;

/**
 * The class representing an elif statement.
 * <p>
 *     This class should not ever exist on its own outside of an if statement,
 *     which is why it has no parse method. For its parsing, go to {@link
 *     IfStatementNode#parse}.
 * </p>
 * @author Patrick Norton
 * @see IfStatementNode
 */
public class ElifStatementNode implements BaseNode {
    private LineInfo lineInfo;
    private TestNode test;
    private StatementBodyNode body;

    /**
     * Construct new instance of ElifStatementNode.
     * @param test The conditional for the statement
     * @param body The body of the statement
     */
    @Contract(pure = true)
    public ElifStatementNode(LineInfo lineInfo, TestNode test, StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.test = test;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getTest() {
        return test;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "elif " + test + " " + body;
    }
}
