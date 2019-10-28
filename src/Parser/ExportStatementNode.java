package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing an export statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see TypegetStatementNode
 */
public class ExportStatementNode extends ImportExportNode {
    /**
     * Create a new instance of ExportStatementNode.
     * @param exports The list of exports
     */
    @Contract(pure = true)
    public ExportStatementNode(LineInfo lineInfo, DottedVariableNode[] exports) {
        super("export", lineInfo, exports, DottedVariableNode.empty(), new DottedVariableNode[0]);
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
        assert tokens.tokenIs(Keyword.EXPORT);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw tokens.error("Empty export statements are illegal");
        }
        DottedVariableNode[] exports = DottedVariableNode.parseNameOnlyList(tokens, false);
        return new ExportStatementNode(info, exports);
    }
}
