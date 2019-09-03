import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a name token.
 * @author Patrick Norton
 */
public class VariableNode implements NameNode {
    private String name;

    @Contract(pure = true)
    public VariableNode(String names) {
        this.name = names;
    }

    @Contract(pure = true)
    public VariableNode() {
        this.name = "";
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return this.name.isEmpty();
    }

    /**
     * Parse a VariableNode if and only if the first token in the list matches
     * one of the ones given.
     * @param tokens The list of tokens to destructively parse
     * @param types The list of types to check against
     * @return The freshly parsed VariableNode
     */
    @NotNull
    static VariableNode parseOnToken(@NotNull TokenList tokens, TokenType... types) {
        if (tokens.tokenIs(types)) {
            return VariableNode.parse(tokens);
        } else {
            return new VariableNode();
        }
    }

    /**
     * Parse a VariableNode from a list of tokens.
     * <p>
     *     The syntax for a VariableNode is: {@code NAME}.
     * </p>
     * @param tokens The list of tokens to destructively parse
     * @return The freshly parsed VariableNode
     */
    @NotNull
    @Contract("_ -> new")
    static VariableNode parse(@NotNull TokenList tokens) {
        if (!tokens.tokenIs(TokenType.NAME)) {
            throw new ParserException("Expected name. got " + tokens.getFirst());
        }
        String name = tokens.getFirst().sequence;
        tokens.nextToken();
        return new VariableNode(name);
    }

    /**
     * Parse the ellipsis unicorn from a list of tokens.
     * <p>
     *     The first item in the list passed must be an {@code ELLIPSIS}.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed ellipsis
     */
    @NotNull
    @Contract("_ -> new")
    static VariableNode parseEllipsis(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.ELLIPSIS);
        tokens.nextToken();
        return new VariableNode("...");
    }

    /**
     * Parse a list of VariableNodes.
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newlines Whether or not to ignore newlines
     * @return The freshly parsed VariableNode array
     */
    static VariableNode[] parseList(TokenList tokens, boolean ignore_newlines) {
        LinkedList<VariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            tokens.passNewlines();
        }
        if (tokens.tokenIs("(") && !tokens.braceContains("in", "for")) {
            tokens.nextToken();
            VariableNode[] vars = parseList(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(VariableNode.parse(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new VariableNode[0]);
    }
}
