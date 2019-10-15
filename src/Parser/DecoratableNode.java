package Parser;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The interface representing a node which can be preceded by a decorator.
 *
 * @author Patrick Norton
 */
public interface DecoratableNode extends IndependentNode {
    void addDecorators(NameNode... decorators);
    NameNode[] getDecorators();

    /**
     * Parse decorators and the decorated node from a list of tokens.
     * <p>
     *     The syntax for a decorator is <code>"$" {@link NameNode}</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DecoratableNode, with decorations added
     */
    @NotNull
    static DecoratableNode parseLeftDecorator(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.AT);
        LinkedList<NameNode> decorators = new LinkedList<>();
        while (tokens.tokenIs(TokenType.AT)) {
            tokens.nextToken();
            decorators.add(NameNode.parse(tokens));
            tokens.passNewlines();
        }
        DecoratableNode stmt = DecoratableNode.parse(tokens);
        stmt.addDecorators(decorators.toArray(new NameNode[0]));
        return stmt;
    }

    /**
     * Parse an decoratable node from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed DecoratableNode
     */
    @NotNull
    static DecoratableNode parse(TokenList tokens) {
        IndependentNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof DecoratableNode) {
            return (DecoratableNode) stmt;
        } else {
            throw tokens.error("Illegal decorator");
        }
    }
}
