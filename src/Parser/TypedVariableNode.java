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

    /**
     * Parse a list of TypedVariableNodes.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypedVariableNode array
     */
    @NotNull
    static TypedVariableNode[] parseList(TokenList tokens) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (true) {
            vars.add(TypedVariableNode.parse(tokens));
            if (tokens.tokenIs(TokenType.ASSIGN, Keyword.IN)) {
                break;
            }
            if (!tokens.tokenIs(",")) {
                throw tokens.error("Unexpected "+tokens.getFirst());
            }
            tokens.nextToken();
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
    @NotNull
    static TypedVariableNode parse(TokenList tokens) {
        TypeLikeNode type = TypeLikeNode.parse(tokens);
        VariableNode var = VariableNode.parse(tokens);
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
            vars.add(TypedVariableNode.parse(tokens));
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
