package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a typed variable.
 * @author Patrick Norton
 * @see TypedArgumentNode
 */
public class TypedVariableNode implements SubTestNode {
    private LineInfo lineInfo;
    private TypeLikeNode type;
    private VariableNode var;

    @Contract(pure = true)
    public TypedVariableNode(TypeLikeNode type, VariableNode var) {
        this(type.getLineInfo(), type, var);
    }

    @Contract(pure = true)
    public TypedVariableNode(LineInfo lineInfo, TypeLikeNode type, VariableNode var) {
        this.lineInfo = lineInfo;
        this.type = type;
        this.var = var;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeLikeNode getType() {
        return type;
    }

    public VariableNode getVar() {
        return var;
    }

    @NotNull
    static TypedVariableNode[] parseListOnToken(@NotNull TokenList tokens, Keyword keyword) {
        if (tokens.tokenIs(keyword)) {
            tokens.nextToken(false);
            return parseList(tokens, false);
        } else {
            return new TypedVariableNode[0];
        }
    }

    /**
     * Parse a list of TypedVariableNodes.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypedVariableNode array
     */
    @NotNull
    static TypedVariableNode[] parseList(TokenList tokens, boolean ignoreNewlines) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (TypeNode.nextIsType(tokens)) {
            vars.add(parse(tokens, ignoreNewlines));
            if (ignoreNewlines) {
                tokens.passNewlines();
            }
            if (!tokens.tokenIs(",")) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    /**
     * Parse a new TypedVariableNode from a list of tokens.
     * <p>
     *     The syntax for a TypedVariableNode is: <code>{@link TypeNode}
     *     {@link VariableNode}</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed list of tokens
     */
    @Contract("_, _ -> new")
    @NotNull
    private static TypedVariableNode parse(TokenList tokens, boolean ignoreNewlines) {
        TypeLikeNode type = TypeLikeNode.parse(tokens, ignoreNewlines);
        VariableNode var = VariableNode.parse(tokens);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        return new TypedVariableNode(type, var);
    }

    /**
     * Parse the typed variables from a for-loop.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed list of tokens
     */
    @NotNull
    static TypedVariableNode[] parseForVars(@NotNull TokenList tokens) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (!tokens.tokenIs(Keyword.IN)) {
            vars.add(TypedVariableNode.parse(tokens, false));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
            }
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    @Override
    public String toString() {
        return type + " " + var;
    }
}
