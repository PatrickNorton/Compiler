import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The class representing the body of a class.
 *
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class ClassBodyNode extends StatementBodyNode {
    private ClassStatementNode[] statements;

    public ClassBodyNode(ClassStatementNode... statements) {
        this.statements = statements;
    }

    @Override
    public ClassStatementNode[] getStatements() {
        return statements;
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
        ArrayList<ClassStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(ClassStatementNode.parse(tokens));
            tokens.passNewlines();
        }
        tokens.nextToken();
        tokens.Newline();
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }
}
