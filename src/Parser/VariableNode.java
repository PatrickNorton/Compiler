package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a name token.
 * @author Patrick Norton
 */
public class VariableNode implements NameNode, EnumKeywordNode {
    private LineInfo lineInfo;
    private String name;

    @Contract(pure = true)
    public VariableNode(LineInfo lineInfo, String names) {
        this.lineInfo = lineInfo;
        this.name = names;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getName() {
        return name;
    }

    @Override
    public VariableNode getVariable() {
        return this;
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static VariableNode empty() {
        return new VariableNode(LineInfo.empty(), "");
    }

    @Override
    public boolean isEmpty() {
        return this.name.isEmpty();
    }

    /**
     * Parse a VariableNode if and only if the first token in the list is a
     * name token.
     *
     * @param tokens The list of tokens to destructively parse
     * @return The freshly parsed VariableNode
     */
    @NotNull
    static VariableNode parseOnName(@NotNull TokenList tokens) {
        return tokens.tokenIs(TokenType.NAME) ? parse(tokens) : empty();
    }

    /**
     * Parse a VariableNode if the first token in the list is the one given.
     *
     * @param tokens The list of tokens to destructively parse
     * @param sentinel The keyword to check for
     * @return The freshly parsed VariableNode
     */
    static VariableNode parseOnToken(@NotNull TokenList tokens, Keyword sentinel) {
        if (tokens.tokenIs(sentinel)) {
            tokens.nextToken();
            return parse(tokens);
        } else {
            return empty();
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
            throw tokens.error("Expected name. got " + tokens.getFirst());
        }
        String name = tokens.tokenSequence();
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        return new VariableNode(info, name);
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
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        return new VariableNode(info, "...");
    }

    public String toString() {
        return this.name;
    }
}
