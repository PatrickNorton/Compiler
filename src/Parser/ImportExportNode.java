package Parser;

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
        if (tokens.tokenIs(Keyword.FROM)) {
            return parseFrom(tokens);
        } else {
            if (!tokens.tokenIs(TokenType.KEYWORD)) {
                throw new ParserException("Unexpected " + tokens.getFirst());
            }
            switch (Keyword.find(tokens.getFirst().sequence)) {
                case IMPORT:
                    return ImportStatementNode.parse(tokens);
                case EXPORT:
                    return ExportStatementNode.parse(tokens);
                case TYPEGET:
                    return TypegetStatementNode.parse(tokens);
                default:
                    throw new RuntimeException("Unknown ImportExportNode");
            }
        }
    }

    @NotNull
    private static ImportExportNode parseFrom(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.FROM);
        if (tokens.lineContains(Keyword.IMPORT)) {
            return ImportStatementNode.parse(tokens);
        } else if (tokens.lineContains(Keyword.EXPORT)) {
            return ExportStatementNode.parse(tokens);
        } else if (tokens.lineContains(Keyword.TYPEGET)) {
            return TypegetStatementNode.parse(tokens);
        } else {
            throw new ParserException("from does not begin a statement");
        }
    }
}
