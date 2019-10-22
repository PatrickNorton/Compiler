package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The interface for all import, export, and typeget nodes.
 * @author Patrick Norton
 */
public abstract class ImportExportNode implements SimpleStatementNode {
    private String type;
    private LineInfo lineInfo;
    private DottedVariableNode[] ports;
    private DottedVariableNode from;
    private DottedVariableNode[] as;

    @Contract(pure = true)
    public ImportExportNode(String type, LineInfo lineInfo, DottedVariableNode[] ports,
                            DottedVariableNode from, DottedVariableNode[] as) {
        this.type = type;
        this.lineInfo = lineInfo;
        this.ports = ports;
        this.from = from;
        this.as = as;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public DottedVariableNode[] getValues() {
        return ports;
    }

    public DottedVariableNode getFrom() {
        return from;
    }

    public DottedVariableNode[] getAs() {
        return as;
    }

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
                throw tokens.error("Unexpected " + tokens.getFirst());
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

    /**
     * Parse an ImportExportNode starting with "from" from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ImportExportNode
     */
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
            throw tokens.error("from does not begin a statement");
        }
    }

    @Override
    public String toString() {
        String ports = TestNode.toString(this.ports);
        String str;
        if (!from.isEmpty()) {
            str = String.format("from %s %s %s", from, type, ports);
        } else {
            str = type + " " + ports;
        }
        if (as.length == 0) {
            return str;
        } else {
            return str + " as " + TestNode.toString(as);
        }
    }
}
