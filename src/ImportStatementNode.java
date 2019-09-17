import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

/**
 * The node representing an import statement.
 * @author Patrick Norton
 * @see ExportStatementNode
 */
public class ImportStatementNode implements ImportExportNode {
    private DottedVariableNode[] imports;
    private DottedVariableNode from;
    private DottedVariableNode[] as;

    /**
     * Create a new instance of ImportStatementNode.
     * @param imports The list of imported names
     * @param from The package from whence they are imported
     */
    @Contract(pure = true)
    public ImportStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this(imports, from, new DottedVariableNode[0]);
    }

    /**
     * Create a new instance of ImportStatementNode,
     * @param imports Tne list of imported names, all top-level
     */
    @Contract(pure = true)
    public ImportStatementNode(DottedVariableNode[] imports, DottedVariableNode from, DottedVariableNode[] as) {
        this.imports = imports;
        this.from = from;
        this.as = as;
    }

    public DottedVariableNode[] getImports() {
        return imports;
    }

    public DottedVariableNode getFrom() {
        return from;
    }

    public DottedVariableNode[] getAs() {
        return as;
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
        if (tokens.tokenIs("from")) {
            tokens.nextToken();
            from = DottedVariableNode.parseNamesOnly(tokens);
        }
        assert tokens.tokenIs("import");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = DottedVariableNode.parseList(tokens, false);
        if (tokens.tokenIs("as")) {
            DottedVariableNode[] as = DottedVariableNode.parseList(tokens, false);
            return new ImportStatementNode(imports, from, as);
        }
        return new ImportStatementNode(imports, from);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (DottedVariableNode d : imports) {
            sj.add(d.toString());
        }
        return (from.isEmpty() ? "" : from + " ") + "import " + sj;
    }
}
