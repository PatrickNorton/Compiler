package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The interface for atomics, e.g. literals, names, and operators.
 * @author Patrick Norton
 */
public interface AtomicNode extends SubTestNode {
    /**
     * Parse a label for a switch statement.
     * <p>
     *     Valid switch statement labels are names, operator types, numeric
     *     literals, and strings.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed switch label.
     */
    @NotNull
    static AtomicNode parseLabel(@NotNull TokenList tokens) {
        switch (tokens.tokenType()) {
            case NAME:
                return NameNode.parse(tokens);
            case OP_FUNC:
                return EscapedOperatorNode.parse(tokens);
            case NUMBER:
                return NumberNode.parse(tokens);
            case STRING:
                return StringLikeNode.parse(tokens);
            default:
                throw tokens.error("Invalid label "+tokens.getFirst());
        }
    }

    /**
     * Parse a list of labels for a switch statement.
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed AtomicNode
     */
    @NotNull
    static AtomicNode[] parseLabelList(@NotNull TokenList tokens) {
        LinkedList<AtomicNode> nodes = new LinkedList<>();
        while (true) {
            nodes.add(parseLabel(tokens));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken();
            if (tokens.tokenIs(TokenType.COLON, "{") || tokens.tokenIs(TokenType.DOUBLE_ARROW)) {
                break;
            }
        }
        if (nodes.isEmpty()) {
            throw tokens.error("Cannot have zero labels");
        }
        return nodes.toArray(new AtomicNode[0]);
    }
}
