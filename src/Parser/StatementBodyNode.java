package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The class representing the body of a statement.
 * @author Patrick Norton
 */
public class StatementBodyNode implements BodyNode {
    private IndependentNode[] statements;

    @Contract(pure = true)
    public StatementBodyNode(IndependentNode... statements) {
        this.statements = statements;
    }

    public IndependentNode[] getStatements() {
        return statements;
    }

    @Override
    public boolean isEmpty() {
        return statements.length == 0;
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
            return StatementBodyNode.parse(tokens);
        } else {
            return new StatementBodyNode();
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
            throw new ParserException("The body of a function must be enclosed in curly brackets");
        }
        tokens.nextToken(true);
        StatementBodyNode st = parseUntilToken(tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return st;
    }

    /**
     * Parse the statements in a fallthrough-allowed switch clause.
     * @param tokens The list of tokens to parse destructively
     * @return The freshly parsed StatementBodyNode
     */
    @NotNull
    @Contract("_ -> new")
    static StatementBodyNode parseCase(@NotNull TokenList tokens) {
        return parseUntilToken(tokens, "case", "default", "}");
    }

    @NotNull
    @Contract("_, _ -> new")
    private static StatementBodyNode parseUntilToken(@NotNull TokenList tokens, String... values) {
        ArrayList<IndependentNode> statements = new ArrayList<>();
        while (!tokens.tokenIs(values)) {
            statements.add(IndependentNode.parse(tokens));
            if (!tokens.tokenIs(values)) {
                tokens.Newline();
            }
        }
        return new StatementBodyNode(statements.toArray(new IndependentNode[0]));
    }

    @Override
    public String toString() {
        if (!isEmpty()) {
            return "{...}";
        } else {
            return "{}";
        }
    }
}
