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
        assert tokens.tokenIs(TokenType.NAME, "(");
        NameNode name;
        if (tokens.tokenIs(TokenType.NAME)) {
            name = VariableNode.parse(tokens);
        } else {
            assert tokens.tokenIs("(");
            tokens.nextToken(true);
            name = parse(tokens);
            if (!tokens.tokenIs(")")) {
                throw tokens.error("Unexpected " + tokens.getFirst());
            }
        }
        name = parsePostBraces(tokens, name);
        if (tokens.tokenIs(TokenType.DOT)) {
            name = DottedVariableNode.fromExpr(tokens, name);
        }
        return name;
    }

    @NotNull
    static NameNode parsePostBraces(@NotNull TokenList tokens, @NotNull NameNode name) {
        TestNode newName = TestNode.parsePostBraces(tokens, name);
        if (newName instanceof NameNode) {
            return (NameNode) newName;
        } else {
            throw tokens.internalError("Error in post-brace parsing");
        }
    }

    static String parenthesize(@NotNull TestNode stmt) {
        return stmt instanceof NameNode ? stmt.toString() : "(" + stmt + ")";
    }
}
