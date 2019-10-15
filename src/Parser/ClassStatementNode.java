package Parser;

import org.jetbrains.annotations.NotNull;

/**
 * The interface for statements which are valid in a class body.
 * <p>
 *     This inherits the method {@code addDescriptor} from
 *     InterfaceStatementNode, this allows all statements in a class body to
 *     add descriptors to themselves
 * </p>
 *
 * @author Patrick Norton
 * @see InterfaceStatementNode
 */
public interface ClassStatementNode extends InterfaceStatementNode {
    /**
     * Parse a ClassStatementNode from a list of tokens.
     * <p>
     *     ClassStatementNodes are simply any {@link BaseNode} which inherits
     *     from this node, and that is how they are parsed, with a call to
     *     {@link IndependentNode#parse}, and a typecast filter. This node
     *     is defined as the union of all its subclasses, check an inheritance
     *     tree or grep for those.
     * </p>
     *
     * @param tokens Tokens to be parsed; parse does operate destructively on
     *               them
     * @return The newly parsed ClassStatementNode
     */
    @NotNull
    static ClassStatementNode parse(@NotNull TokenList tokens) {
        if (tokens.tokenIs("static") && tokens.tokenIs(1, "{")) {
            return StaticBlockNode.parse(tokens);
        }
        BaseNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof ClassStatementNode) {
            return (ClassStatementNode) stmt;
        }
        throw tokens.error(tokens.getFirst() + " is not a valid class statement");
    }
}
