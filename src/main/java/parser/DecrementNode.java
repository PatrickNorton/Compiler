package main.java.parser;

/**
 * The class for decrementing a variable.
 *
 * @author Patrick Norton
 * @see IncrementNode
 */
public class DecrementNode implements IncDecNode {
    private LineInfo lineInfo;
    private NameNode variable;

    public DecrementNode(NameNode variable) {
        this(variable.getLineInfo(), variable);
    }

    /**
     * Create new DecrementNode from a variable.
     * @param variable The decremented variable
     */

    public DecrementNode(LineInfo lineInfo, NameNode variable) {
        this.lineInfo = lineInfo;
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
     * Parse a DecrementNode from a list of tokens.
     * <p>
     *     The syntax for a DecrementNode is: <code>{@link NameNode} "--"
     *     </code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed node
     */

    static DecrementNode parse(TokenList tokens) {
        NameNode var = NameNode.parse(tokens);
        if (!tokens.tokenIs("--")) {
            throw tokens.internalError("Expected --, got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new DecrementNode(var);
    }

    @Override
    public String toString() {
        return variable + "--";
    }
}
