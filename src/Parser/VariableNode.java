package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a name token.
 * @author Patrick Norton
 */
public class VariableNode implements NameNode, EnumKeywordNode {
    private String name;

    @Contract(pure = true)
    public VariableNode(String names) {
        this.name = names;
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static VariableNode empty() {
        return new VariableNode("");
    }

    public boolean isEmpty() {
        return this.name.isEmpty();
    }

    /**
     * Parse a Parser.VariableNode if and only if the first token in the list matches
     * one of the ones given.
     * @param tokens The list of tokens to destructively parse
     * @param types The list of types to check against
     * @return The freshly parsed Parser.VariableNode
     */
    @NotNull
    static VariableNode parseOnToken(@NotNull TokenList tokens, TokenType... types) {
        if (tokens.tokenIs(types)) {
            return VariableNode.parse(tokens);
        } else {
            return VariableNode.empty();
        }
    }

    /**
     * Parse a Parser.VariableNode from a list of tokens.
     * <p>
     *     The syntax for a Parser.VariableNode is: {@code NAME}.
     * </p>
     * @param tokens The list of tokens to destructively parse
     * @return The freshly parsed Parser.VariableNode
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
     * @return The freshly parsed Parser.VariableNode array
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

    public String toString() {
        return this.name;
    }
}