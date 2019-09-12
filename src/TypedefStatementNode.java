import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a typedef statement.
 * @author Patrick Norton
 */
public class TypedefStatementNode implements SimpleStatementNode {
    private TypeNode name;
    private TypeNode type;

    /**
     * Construct a new instance of TypedefStatementNode.
     * @param name The name of the typedef
     * @param type The type being assigned to
     */
    @Contract(pure = true)
    public TypedefStatementNode(TypeNode name, TypeNode type) {
        this.name = name;
        this.type = type;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode getType() {
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
        assert tokens.tokenIs("typedef");
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        assert tokens.tokenIs("as");
        tokens.nextToken();
        TypeNode type = TypeNode.parse(tokens);
        return new TypedefStatementNode(name, type);
    }

    @Override
    public String toString() {
        return "typedef " + name + " as " + type;
    }
}
