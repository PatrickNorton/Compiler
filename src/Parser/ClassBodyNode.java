package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The class representing the body of a class.
 *
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class ClassBodyNode implements BodyNode {
    private ClassStatementNode[] statements;

    @Contract(pure = true)
    public ClassBodyNode(ClassStatementNode... statements) {
        this.statements = statements;
    }

    @Override
    public ClassStatementNode[] getStatements() {
        return statements;
    }

    @Override
    public boolean isEmpty() {
        return statements.length == 0;
    }

    /**
     * Parse ClassBodyNode from list of tokens.
     * <p>
     *     A class body is simply a list of class statements, e.g. statements
     *     which can have a declaration in front of them. The statement must
     *     also be surrounded by curly braces, not doing so will result in a
     *     ParserException being raised. The syntax of a ClassStatementNode is
     *     <code> "{" *{@link ClassStatementNode} "}" NEWLINE</code>.
     * </p>
     * @param tokens The list of tokens passed
     * @return The parsed ClassBodyNode
     */
    @NotNull
    @Contract("_ -> new")
    static ClassBodyNode parse(@NotNull TokenList tokens) {
        // FIXME: Allow lack of newlines around braces
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        tokens.nextToken(true);
        ClassBodyNode cb = parseUntilToken(tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return cb;
    }

    /**
     * Parse the ClassBodyNode of an enum from a list of tokens.
     * <p>
     *     This is functionally identical to the normal parse method, except
     *     there is no check for the opening brace, as that will have already
     *     been parsed before the constants were enumerated.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ClassBodyNode
     */
    @NotNull
    static ClassBodyNode parseEnum(@NotNull TokenList tokens) {
        ClassBodyNode cb = parseUntilToken(tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return cb;
    }

    @NotNull
    @Contract("_, _ -> new")
    private static ClassBodyNode parseUntilToken(@NotNull TokenList tokens, String... sentinels) {
        ArrayList<ClassStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs(sentinels)) {
            statements.add(ClassStatementNode.parse(tokens));
            if (!tokens.tokenIs(sentinels)) {
                tokens.Newline();
            }
        }
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }

    static ClassBodyNode fromList(LinkedList<ClassStatementNode> statements) {
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }

    @Override
    public String toString() {
        return isEmpty() ? "{}" : "{...}";
    }
}
