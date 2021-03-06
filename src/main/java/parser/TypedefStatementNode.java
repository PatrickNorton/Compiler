package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a typedef statement.
 * @author Patrick Norton
 */
public class TypedefStatementNode implements SimpleStatementNode {
    private LineInfo lineInfo;
    private TypeNode name;
    private TypeLikeNode type;

    /**
     * Construct a new instance of TypedefStatementNode.
     * @param name The name of the typedef
     * @param type The type being assigned to
     */
    @Contract(pure = true)
    public TypedefStatementNode(LineInfo lineInfo, TypeNode name, TypeLikeNode type) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.type = type;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeLikeNode getType() {
        return type;
    }

    /**
     * Parse a TypedefStatementNode from a list of tokens.
     * <p>
     *     The syntax for a typedef statement is: <code>"typedef" {@link
     *     TypeNode} "as" {@link TypeNode}</code>. The list of tokens passed
     *     must begin with "typedef".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypedefStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static TypedefStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.TYPEDEF);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        assert tokens.tokenIs(Keyword.AS);
        tokens.nextToken();
        TypeLikeNode type = TypeLikeNode.parse(tokens);
        return new TypedefStatementNode(info, name, type);
    }

    @Override
    public String toString() {
        return "typedef " + name + " as " + type;
    }
}
