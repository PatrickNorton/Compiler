package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class for decrementing a variable.
 *
 * @author Patrick Norton
 * @see IncrementNode
 */
public class DecrementNode implements SimpleStatementNode {
    private LineInfo lineInfo;
    private NameNode variable;

    public DecrementNode(NameNode variable) {
        this(variable.getLineInfo(), variable);
    }

    /**
     * Create new DecrementNode from a variable.
     * @param variable The decremented variable
     */
    @Contract(pure = true)
    public DecrementNode(LineInfo lineInfo, NameNode variable) {
        this.lineInfo = lineInfo;
        this.variable = variable;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public NameNode getVariable() {
        return variable;
    }

    /**
     * Parse a DecrementNode from a list of tokens.
     * <p>
     *     The syntax for a DecrementNode is: <code>{@link NameNode} "--"
     *     </code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed node
     */
    @NotNull
    @Contract("_ -> new")
    static DecrementNode parse(TokenList tokens) {
        NameNode var = NameNode.parse(tokens);
        if (!tokens.tokenIs("--")) {
            throw new RuntimeException("Expected --, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DecrementNode(var);
    }

    @Override
    public String toString() {
        return variable + "--";
    }
}
