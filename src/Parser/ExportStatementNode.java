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
    public ExportStatementNode(LineInfo lineInfo, DottedVariableNode[] exports, DottedVariableNode from) {
        super("export", lineInfo, exports, from, new DottedVariableNode[0]);
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
        DottedVariableNode from = DottedVariableNode.empty();
        LineInfo info = null;
        if (tokens.tokenIs(Keyword.FROM)) {
            info = tokens.lineInfo();
            tokens.nextToken();
            from = DottedVariableNode.parseNamesOnly(tokens);
        }
        assert tokens.tokenIs(Keyword.EXPORT);
        if (info == null) {
            info = tokens.lineInfo();
        }
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw tokens.error("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = DottedVariableNode.parseNameOnlyList(tokens);
        return new ExportStatementNode(info, imports, from);
    }
}
