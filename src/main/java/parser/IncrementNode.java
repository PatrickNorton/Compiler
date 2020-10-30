package main.java.parser;

/**
 * The class for incrementing a variable.
 * @author Patrick Norton
 * @see DecrementNode
 */
public class IncrementNode implements IncDecNode {
    private LineInfo lineInfo;
    private NameNode variable;

    public IncrementNode(NameNode variable) {
        this(variable.getLineInfo(), variable);
    }

    /**
     * Construct a new instance of IncrementNode.
     * @param variable The variable to be incremented
     */

    public IncrementNode(LineInfo info, NameNode variable) {
        this.lineInfo = info;
        this.variable = variable;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
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

    static IncrementNode parse(TokenList tokens) {
        NameNode var = NameNode.parse(tokens);
        if (!tokens.tokenIs("++")) {
            throw tokens.internalError("Expected ++, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new IncrementNode(var);
    }

    @Override
    public String toString() {
        return variable + "++";
    }
}
