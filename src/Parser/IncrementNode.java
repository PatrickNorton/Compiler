package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class for incrementing a variable.
 * @author Patrick Norton
 * @see DecrementNode
 */
public class IncrementNode implements SimpleStatementNode {
    private LineInfo lineInfo;
    private NameNode variable;

    public IncrementNode(NameNode variable) {
        this(variable.getLineInfo(), variable);
    }

    /**
     * Construct a new instance of IncrementNode.
     * @param variable The variable to be incremented
     */
    @Contract(pure = true)
    public IncrementNode(LineInfo info, NameNode variable) {
        this.lineInfo = info;
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
     * Parse an increment from a list of tokens.
     * <p>
     *     The grammar for an increment is: <code>{@link NameNode} "++"</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed IncrementNode
     */
    @NotNull
    @Contract("_ -> new")
    static IncrementNode parse(TokenList tokens) {
        NameNode var = DottedVariableNode.parseName(tokens);
        if (!tokens.tokenIs("++")) {
            throw new RuntimeException("Expected ++, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new IncrementNode(var);
    }

    @Override
    public String toString() {
        return variable + "++";
    }
}
