package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The interface for atomics, e.g. literals, names, and operators.
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
                throw new ParserException("Invalid label "+tokens.getFirst());
        }
    }

    /**
     * Parse a list of labels for a switch statement.
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed Parser.AtomicNode
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
            if (tokens.tokenIs(TokenType.COLON, "{")) {
                break;
            }
        }
        if (nodes.isEmpty()) {
            throw new ParserException("Cannot have zero labels");
        }
        return nodes.toArray(new AtomicNode[0]);
    }
}
