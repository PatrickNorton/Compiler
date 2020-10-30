package main.java.parser;

import java.util.LinkedList;

/**
 * The class representing a typed variable.
 * @author Patrick Norton
 * @see TypedArgumentNode
 */
public class TypedVariableNode implements VarLikeNode, SubTestNode {
    private LineInfo lineInfo;
    private TypeLikeNode type;
    private VariableNode var;

    public TypedVariableNode(TypeLikeNode type, VariableNode var) {
        this(type.getLineInfo(), type, var);
    }

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

    @Override
    public VariableNode getVariable() {
        return var;
    }

    @Override
    public boolean isTyped() {
        return true;
    }

    static TypedVariableNode[] parseListOnToken(TokenList tokens, Keyword keyword) {
        if (tokens.tokenIs(keyword)) {
            tokens.nextToken(false);
            return parseList(tokens);
        } else {
            return new TypedVariableNode[0];
        }
    }

    /**
     * Parse a list of TypedVariableNodes.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypedVariableNode array
     */

    static TypedVariableNode[] parseList(TokenList tokens) {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (TypeNode.nextIsType(tokens)) {
            vars.add(parse(tokens, false));
            if (!tokens.tokenIs(",")) {
                break;
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

    static TypedVariableNode parse(TokenList tokens, boolean ignoreNewlines) {
        TypeLikeNode type = TypeLikeNode.parse(tokens, ignoreNewlines);
        VariableNode var = VariableNode.parse(tokens);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        return new TypedVariableNode(type, var);
    }

    @Override
    public String toString() {
        return type + " " + var;
    }
}
