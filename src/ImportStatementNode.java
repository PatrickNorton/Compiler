import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing an import statement.
 * @author Patrick Norton
 * @see ExportStatementNode
 */
public class ImportStatementNode implements ImportExportNode {
    private DottedVariableNode[] imports;
    private DottedVariableNode from;

    /**
     * Create a new instance of ImportStatementNode.
     * @param imports The list of imported names
     * @param from The package from whence they are imported
     */
    @Contract(pure = true)
    public ImportStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this.imports = imports;
        this.from = from;
    }

    /**
     * Create a new instance of ImportStatementNode,
     * @param imports Tne list of imported names, all top-level
     */
    @Contract(pure = true)
    public ImportStatementNode(DottedVariableNode[] imports) {
        this.imports = imports;
        this.from = new DottedVariableNode();
    }

    public DottedVariableNode[] getImports() {
        return imports;
    }

    public DottedVariableNode getFrom() {
        return from;
    }

    /**
     * Parse an import statement from a list of tokens.
     * <p>
     *     The grammar for an import statement is: <code>["from" {@link
     *     DottedVariableNode}] "import" {@link DottedVariableNode} *(","
     *     {@link DottedVariableNode}) [","]</code>. The list of tokens
     *     must either begin with "import" or "from".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ReturnStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static ImportStatementNode parse(@NotNull TokenList tokens) {
        // FIXME: Does not parse "from" statements properly
        assert tokens.tokenIs("import");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new ImportStatementNode(imports);
    }
}
