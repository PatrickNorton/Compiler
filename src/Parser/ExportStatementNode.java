package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

/**
 * The node representing an export statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see TypegetStatementNode
 */
public class ExportStatementNode implements ImportExportNode {
    private DottedVariableNode[] exports;

    /**
     * Create a new instance of Parser.ExportStatementNode.
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
     * Parse a new Parser.ExportStatementNode from a list of tokens.
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
        return new ExportStatementNode(exports);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (DottedVariableNode d : exports) {
            sj.add(d.toString());
        }
        return "export " + sj;
    }
}
