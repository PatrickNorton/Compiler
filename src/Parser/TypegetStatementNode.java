package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a typeget statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see ExportStatementNode
 */
public class TypegetStatementNode extends ImportExportNode {
    @Contract(pure = true)
    public TypegetStatementNode(LineInfo lineInfo, DottedVariableNode[] imports, DottedVariableNode from) {
        this(lineInfo, imports, from, new DottedVariableNode[0]);
    }

    @Contract(pure = true)
    public TypegetStatementNode(LineInfo lineInfo, DottedVariableNode[] imports, DottedVariableNode from, DottedVariableNode[] as) {
        super("typeget", lineInfo, imports, from, as);
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
        LineInfo lineInfo = null;
        DottedVariableNode from = DottedVariableNode.empty();
        if (tokens.tokenIs(Keyword.FROM)) {
            lineInfo = tokens.lineInfo();
            tokens.nextToken();
            from = DottedVariableNode.parseName(tokens);
        }
        assert tokens.tokenIs(Keyword.TYPEGET);
        if (lineInfo == null) {
            lineInfo = tokens.lineInfo();
        }
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw tokens.error("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = DottedVariableNode.parseList(tokens, false);
        if (tokens.tokenIs(Keyword.AS)) {
            tokens.nextToken();
            DottedVariableNode[] as = DottedVariableNode.parseList(tokens, false);
            return new TypegetStatementNode(lineInfo, typegets, from, as);
        }
        return new TypegetStatementNode(lineInfo, typegets, from);
    }
}
