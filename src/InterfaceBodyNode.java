import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The node representing an interface body.
 * @author Patrick Norton
 */
public class InterfaceBodyNode extends StatementBodyNode {
    private InterfaceStatementNode[] statements;

    public InterfaceBodyNode(InterfaceStatementNode... statements) {
        this.statements = statements;
    }

    @Override
    public InterfaceStatementNode[] getStatements() {
        return statements;
    }

    /**
     * Parse an interface body from a list of tokens.
     * <p>
     *     The syntax for an interface body is: <code>"{" {@link
     *     InterfaceStatementNode} *(NEWLINE {@link InterfaceStatementNode}
     *     "}"</code>.
     * </p>
     * @param tokens
     * @return
     */
    @NotNull
    @Contract("_ -> new")
    static InterfaceBodyNode parse(@NotNull TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        tokens.nextToken(true);
        ArrayList<InterfaceStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(InterfaceStatementNode.parse(tokens));
            if (!tokens.tokenIs("}")) {
                tokens.Newline();
            }
        }
        tokens.nextToken();
        return new InterfaceBodyNode(statements.toArray(new InterfaceStatementNode[0]));
    }

    @Override
    public String toString() {
        return "{...}";
    }
}
