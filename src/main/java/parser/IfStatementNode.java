package main.java.parser;

import java.util.LinkedList;

/**
 * The class representing an if-elif-else statement.
 * @author Patrick Norton
 * @see ElifStatementNode
 */
public class IfStatementNode implements FlowStatementNode {
    private LineInfo lineInfo;
    private TestNode conditional;
    private VariableNode as;
    private StatementBodyNode body;
    private ElifStatementNode[] elifs;
    private StatementBodyNode elseStmt;

    /**
     * Create a new instance of IfStatementNode.
     * @param conditional The conditional to test
     * @param body The body of the initial if-statement
     * @param elifs All elif statements post-ceding the initial if
     * @param elseStmt The else statement at the end
     */

    public IfStatementNode(LineInfo lineInfo, TestNode conditional, VariableNode as, StatementBodyNode body,
                           ElifStatementNode[] elifs, StatementBodyNode elseStmt) {
        this.lineInfo = lineInfo;
        this.conditional = conditional;
        this.as = as;
        this.body = body;
        this.elifs = elifs;
        this.elseStmt = elseStmt;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getConditional() {
        return conditional;
    }

    public VariableNode getAs() {
        return as;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public ElifStatementNode[] getElifs() {
        return elifs;
    }

    public StatementBodyNode getElseStmt() {
        return elseStmt;
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

    static IfStatementNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.IF);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode test = TestNode.parse(tokens);
        VariableNode as = VariableNode.parseOnToken(tokens, Keyword.AS);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        LinkedList<ElifStatementNode> elifs = new LinkedList<>();
        while (tokens.tokenIs(Keyword.ELIF)) {
            tokens.nextToken();
            TestNode elifCondition = TestNode.parse(tokens);
            VariableNode elifAs = VariableNode.parseOnToken(tokens, Keyword.AS);
            StatementBodyNode elifBody = StatementBodyNode.parse(tokens);
            elifs.add(new ElifStatementNode(info, elifCondition, elifAs, elifBody));
        }
        StatementBodyNode elseStmt = StatementBodyNode.parseOnToken(tokens, "else");
        return new IfStatementNode(info, test, as, body, elifs.toArray(new ElifStatementNode[0]), elseStmt);
    }

    @Override
    public String toString() {
        return "if " + conditional + (as.isEmpty() ? "" : " as " + as) + " " + body;
    }
}
