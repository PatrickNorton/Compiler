package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    @Contract(pure = true)
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

    @NotNull
    @Contract(" -> new")
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
    @NotNull
    @Contract("_, _ -> new")
    static StatementBodyNode parseOnToken(@NotNull TokenList tokens, String... types) {
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
    @NotNull
    @Contract("_, _ -> new")
    static StatementBodyNode parseOnToken(@NotNull TokenList tokens, Keyword... types) {
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
    @NotNull
    @Contract("_ -> new")
    static StatementBodyNode parse(@NotNull TokenList tokens) {
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

    @NotNull
    @Contract("_, _, _ -> new")
    private static StatementBodyNode parseUntilToken(LineInfo lineInfo, @NotNull TokenList tokens, String... values) {
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

    @NotNull
    @Override
    public Iterator<IndependentNode> iterator() {
        return Arrays.asList(statements).iterator();
    }
}
