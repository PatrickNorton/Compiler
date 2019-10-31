package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

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
        if (tokens.tokenIs("(") && !tokens.braceContains(Keyword.IN, Keyword.FOR)) {
            tokens.nextToken();
            VariableNode[] vars = parseList(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw tokens.error("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw tokens.error("Unmatched braces");
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
