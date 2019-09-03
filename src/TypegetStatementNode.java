import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a typeget statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see ExportStatementNode
 */
public class TypegetStatementNode implements ImportExportNode {
    private DottedVariableNode[] typegets;
    private DottedVariableNode from;

    @Contract(pure = true)
    public TypegetStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this.typegets = imports;
        this.from = from;
    }

    @Contract(pure = true)
    public TypegetStatementNode(DottedVariableNode[] imports) {
        this.typegets = imports;
        this.from = null;
    }

    public DottedVariableNode[] getTypegets() {
        return typegets;
    }

    public DottedVariableNode getFrom() {
        return from;
    }

    /**
     * Parse a TypegetStatementNode from a list of tokens.
     * <p>
     *     The syntax for a typeget statement is: <code>["from" {@link
     *     DottedVariableNode}] "typeget" {@link DottedVariableNode} *(","
     *     {@link DottedVariableNode}) [","]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypegetStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static TypegetStatementNode parse(@NotNull TokenList tokens) {
        DottedVariableNode from = new DottedVariableNode();
        if (tokens.tokenIs("from")) {
            tokens.nextToken();
            from = DottedVariableNode.parse(tokens);
        }
        assert tokens.tokenIs("typeget");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new TypegetStatementNode(typegets, from);
    }
}
