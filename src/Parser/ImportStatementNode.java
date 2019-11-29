package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing an import statement.
 * @author Patrick Norton
 * @see ExportStatementNode
 */
public class ImportStatementNode extends ImportExportNode {
    /**
     * Create a new instance of ImportStatementNode.
     * @param imports The list of imported names
     * @param from The package from whence they are imported
     */
    @Contract(pure = true)
    public ImportStatementNode(LineInfo info, DottedVariableNode[] imports, DottedVariableNode from, int preDots) {
        this(info, imports, from, new DottedVariableNode[0], preDots);
    }

    /**
     * Create a new instance of ImportStatementNode,
     * @param imports Tne list of imported names, all top-level
     */
    @Contract(pure = true)
    public ImportStatementNode(LineInfo info, DottedVariableNode[] imports, DottedVariableNode from, DottedVariableNode[] as, int preDots) {
        super("import", info, imports, from, as, preDots);
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
        DottedVariableNode from = DottedVariableNode.empty();
        LineInfo info = null;
        int preDots = 0;
        if (tokens.tokenIs(Keyword.FROM)) {
            info = tokens.lineInfo();
            tokens.nextToken();
            preDots = parsePreDots(tokens);
            from = DottedVariableNode.parseNamesOnly(tokens);
        }
        assert tokens.tokenIs(Keyword.IMPORT);
        if (info == null) {
            info = tokens.lineInfo();
        }
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw tokens.error("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = DottedVariableNode.parseNameOnlyList(tokens);
        if (tokens.tokenIs(Keyword.AS)) {
            DottedVariableNode[] as = DottedVariableNode.parseNameOnlyList(tokens);
            return new ImportStatementNode(info, imports, from, as, preDots);
        }
        return new ImportStatementNode(info, imports, from, preDots);
    }
}
