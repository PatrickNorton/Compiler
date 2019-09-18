import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The interface for all import, export, and typeget nodes.
 * @author Patrick Norton
 */
public interface ImportExportNode extends SimpleStatementNode {
    /**
     * Parse an ImportExportNode from a list of tokens.
     * <p>
     *     This method at the moment only parses the nodes that begin with
     *     {@code "from"}, and nothing else.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ImportExportNode
     */
    @Contract("_ -> new")
    @NotNull
    static ImportExportNode parse(@NotNull TokenList tokens) {
        if (tokens.tokenIs("from")) {
            return parseFrom(tokens);
        } else {
            switch (tokens.getFirst().sequence) {
                case "import":
                    return ImportStatementNode.parse(tokens);
                case "export":
                    return ExportStatementNode.parse(tokens);
                case "typeget":
                    return TypegetStatementNode.parse(tokens);
                default:
                    throw new RuntimeException("Unknown ImportExportNode");
            }
        }
    }

    @NotNull
    private static ImportExportNode parseFrom(@NotNull TokenList tokens) {
        assert tokens.tokenIs("from");
        if (tokens.lineContains("import")) {
            return ImportStatementNode.parse(tokens);
        } else if (tokens.lineContains("export")) {
            return ExportStatementNode.parse(tokens);
        } else if (tokens.lineContains("typeget")) {
            return TypegetStatementNode.parse(tokens);
        } else {
            throw new ParserException("from does not begin a statement");
        }
    }
}
