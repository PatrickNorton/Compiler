import org.jetbrains.annotations.NotNull;

/**
 * The interface for atomics, e.g. literals, names, and operators.
 *
 * @author Patrick Norton
 */
public interface AtomicNode extends SubTestNode {
    @NotNull
    static AtomicNode parseLabel(@NotNull TokenList tokens) {
        switch (tokens.getFirst().token) {
            case NAME:
                return NameNode.parse(tokens);
            case OP_FUNC:
                return OperatorTypeNode.parse(tokens);
            case NUMBER:
                return NumberNode.parse(tokens);
            case STRING:
                return StringNode.parse(tokens);
            default:
                throw new ParserException("Expected valid label, got "+tokens.getFirst());
        }
    }
}
