package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The node representing an interface body.
 * @author Patrick Norton
 */
public class InterfaceBodyNode implements BodyNode {
    private LineInfo lineInfo;
    private InterfaceStatementNode[] statements;

    @Contract(pure = true)
    public InterfaceBodyNode(LineInfo lineInfo, InterfaceStatementNode... statements) {
        this.lineInfo = lineInfo;
        this.statements = statements;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public InterfaceStatementNode[] getStatements() {
        return statements;
    }

    @Override
    public boolean isEmpty() {
        return statements.length == 0;
    }

    @Override
    public InterfaceStatementNode get(int i) {
        return statements[i];
    }

    /**
     * Parse an interface body from a list of tokens.
     * <p>
     *     The syntax for an interface body is: <code>"{" {@link
     *     InterfaceStatementNode} *(NEWLINE {@link InterfaceStatementNode}
     *     "}"</code>.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed InterfaceBodyNode
     */
    @NotNull
    @Contract("_ -> new")
    static InterfaceBodyNode parse(@NotNull TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw tokens.error("The body of a class must be enclosed in curly brackets");
        }
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        ArrayList<InterfaceStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(InterfaceStatementNode.parse(tokens));
            if (!tokens.tokenIs("}")) {
                tokens.Newline();
            }
        }
        tokens.nextToken();
        return new InterfaceBodyNode(info, statements.toArray(new InterfaceStatementNode[0]));
    }

    @Override
    public String toString() {
        return isEmpty() ? "{}" : "{...}";
    }
}
