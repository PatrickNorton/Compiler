import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing an export statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see TypegetStatementNode
 */
public class ExportStatementNode implements ImportExportNode {
    private DottedVariableNode[] exports;

    /**
     * Create a new instance of ExportStatementNode.
     * @param exports The list of exports
     */
    @Contract(pure = true)
    public ExportStatementNode(DottedVariableNode[] exports) {
        this.exports = exports;
    }

    public DottedVariableNode[] getExports() {
        return exports;
    }

    /**
     * Parse a new ExportStatementNode from a list of tokens.
     * <p>
     *     The syntax for an export statement is: <code>"export" {@link
     *     DottedVariableNode} *("," {@link DottedVariableNode}) [","]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed token
     */
    @NotNull
    @Contract("_ -> new")
    static ExportStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("export");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty export statements are illegal");
        }
        DottedVariableNode[] exports = DottedVariableNode.parseList(tokens, false);
        tokens.Newline();
        return new ExportStatementNode(exports);
    }
}
