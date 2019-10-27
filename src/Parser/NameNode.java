package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The interface for any sort of name, such as a function call, index, or
 * variable.
 * @author Patrick Norton
 */
public interface NameNode extends AtomicNode, PostDottableNode, AssignableNode {
    /**
     * Parse a name from a list of tokens.
     * <p>
     *     The syntax for a NameNode is made up of its constituent subclasses.
     *     The list of tokens must begin with a NAME token.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed NameNode
     */
    @NotNull
    @Contract("_ -> new")
    static NameNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = VariableNode.parse(tokens);
        while_brace:
        while (tokens.tokenIs(TokenType.OPEN_BRACE)) {
            switch (tokens.tokenSequence()) {
                case "(":
                    name = new FunctionCallNode(name, ArgumentNode.parseList(tokens));
                    break;
                case "[":
                    if (tokens.braceContains(":")) {
                        name = new IndexNode(name, SliceNode.parse(tokens));
                    } else {
                        name = new IndexNode(name, LiteralNode.parse(tokens).getBuilders());
                    }
                    break;
                case "{":
                    break while_brace;
                default:
                    throw new RuntimeException("Unexpected brace");
            }
        }
        if (tokens.tokenIs(TokenType.DOT)) {
            name = DottedVariableNode.fromExpr(tokens, name);
        }
        return name;
    }
}
