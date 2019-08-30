import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class for decrementing a variable.
 *
 * @author Patrick Norton
 * @see IncrementNode
 */
public class DecrementNode implements SimpleStatementNode {
    private NameNode variable;

    /**
     * Create new DecrementNode from a variable.
     * @param variable The decremented variable
     */
    @Contract(pure = true)
    public DecrementNode(NameNode variable) {
        this.variable = variable;
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
}
