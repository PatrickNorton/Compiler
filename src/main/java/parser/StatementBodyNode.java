package main.java.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * The class representing the body of a statement.
 * @author Patrick Norton
 */
public class StatementBodyNode implements BodyNode, Iterable<IndependentNode> {
    private LineInfo lineInfo;
    private IndependentNode[] statements;

    public StatementBodyNode() {
        this(LineInfo.empty());
    }

    public StatementBodyNode(IndependentNode... statements) {
        this(statements[0].getLineInfo(), statements);
    }

    public StatementBodyNode(LineInfo lineInfo, IndependentNode... statements) {
        this.lineInfo = lineInfo;
        this.statements = statements;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public IndependentNode[] getStatements() {
        return statements;
    }

    @Override
    public boolean isEmpty() {
        return statements.length == 0;
    }

    @Override
    public IndependentNode get(int i) {
        return statements[i];
    }

    public static StatementBodyNode empty() {
        return new StatementBodyNode();
    }

    /**
     * Parse a StatementBodyNode iff the next token matches those passed.
     * <p>
     *     This method will parse the token it is testing for if it matches.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param types The tokens to parse on
     * @return The freshly parsed StatementBodyNode
     */

    static StatementBodyNode parseOnToken(TokenList tokens, String... types) {
        if (tokens.tokenIs(types)) {
            tokens.nextToken();
            return parse(tokens);
        } else {
            return empty();
        }
    }

    /**
     * Parse a StatementBodyNode iff the next token matches those passed.
     * <p>
     *     This method will parse the token it is testing for if it matches.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param types The tokens to parse on
     * @return The freshly parsed StatementBodyNode
     */

    static StatementBodyNode parseOnToken(TokenList tokens, Keyword... types) {
        if (tokens.tokenIs(types)) {
            tokens.nextToken();
            return parse(tokens);
        } else {
            return empty();
        }
    }

    /**
     * Parse a StatementBodyNode from a list of tokens.
     * <p>
     *     The syntax for a StatementBodyNode is: <code>"{" {@link BaseNode}
     *     *(NEWLINE {@link BaseNode}) [NEWLINE] "}"</code>
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed StatementBodyNode
     */

    static StatementBodyNode parse(TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw tokens.error("The body of a function must be enclosed in curly brackets");
        }
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken(true);
        StatementBodyNode st = parseUntilToken(lineInfo, tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return st;
    }

    private static StatementBodyNode parseUntilToken(LineInfo lineInfo,TokenList tokens, String... values) {
        ArrayList<IndependentNode> statements = new ArrayList<>();
        while (!tokens.tokenIs(values)) {
            statements.add(IndependentNode.parse(tokens));
            if (!tokens.tokenIs(values)) {
                tokens.Newline();
            }
        }
        return new StatementBodyNode(lineInfo, statements.toArray(new IndependentNode[0]));
    }

    @Override
    public String toString() {
        return isEmpty() ? "{}" : "{...}";
    }

    @Override
    public Iterator<IndependentNode> iterator() {
        return Arrays.asList(statements).iterator();
    }
}
