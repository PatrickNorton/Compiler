package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class for all import, export, and typeget nodes.
 * @author Patrick Norton
 */
public class ImportExportNode implements SimpleStatementNode {
    public static final Type IMPORT = Type.IMPORT;
    public static final Type EXPORT = Type.EXPORT;
    public static final Type TYPEGET = Type.TYPEGET;

    private Type type;
    private LineInfo lineInfo;
    private DottedVariableNode[] ports;
    private DottedVariableNode from;
    private DottedVariableNode[] as;
    private int preDots;
    private boolean isWildcard;

    public ImportExportNode(Type type, LineInfo lineInfo, DottedVariableNode from, int preDots, boolean isWildcard) {
        this(type, lineInfo, new DottedVariableNode[0], from, preDots);
        this.isWildcard = isWildcard;
    }

    public ImportExportNode(Type type, LineInfo lineInfo, DottedVariableNode[] ports,
                            DottedVariableNode from, int preDots) {
        this(type, lineInfo, ports, from, new DottedVariableNode[0], preDots);
    }

    @Contract(pure = true)
    public ImportExportNode(Type type, LineInfo lineInfo, DottedVariableNode[] ports,
                            DottedVariableNode from, DottedVariableNode[] as, int preDots) {
        this.type = type;
        this.lineInfo = lineInfo;
        this.ports = ports;
        this.from = from;
        this.as = as;
        this.preDots = preDots;
    }

    public ImportExportNode.Type getType() {
        return type;
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

    public int getPreDots() {
        return preDots;
    }

    public boolean isWildcard() {
        return isWildcard;
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
        DottedVariableNode from = DottedVariableNode.empty();
        LineInfo info = null;
        int preDots = 0;
        if (tokens.tokenIs(Keyword.FROM)) {
            info = tokens.lineInfo();
            tokens.nextToken();
            preDots = parsePreDots(tokens);
            from = DottedVariableNode.parseOnName(tokens);
        }
        assert tokens.tokenIs(Keyword.IMPORT, Keyword.EXPORT, Keyword.TYPEGET);
        if (info == null) {
            info = tokens.lineInfo();
        }
        String strType = tokens.tokenSequence();
        Type type;
        switch (strType) {
            case "import":
                type = Type.IMPORT;
                break;
            case "export":
                type = Type.EXPORT;
                break;
            case "typeget":
                type = Type.TYPEGET;
                break;
            default:
                throw new RuntimeException("Unknown type for ImportExportNode: " + strType);
        }
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE, TokenType.EPSILON)) {
            throw tokens.errorf("Empty %s statements are illegal", type);
        }
        if (tokens.tokenIs("*")) {
            tokens.nextToken();
            if (tokens.tokenIs(Keyword.AS)) {
                throw tokens.error("Cannot use 'as' with wildcard");
            }
            return new ImportExportNode(type, info, from, preDots,  true);
        }
        DottedVariableNode[] imports = DottedVariableNode.parseNameOnlyList(tokens);
        if (tokens.tokenIs(Keyword.AS)) {
            tokens.nextToken();
            DottedVariableNode[] as = DottedVariableNode.parseNameOnlyList(tokens);
            return new ImportExportNode(type, info, imports, from, as, preDots);
        }
        return new ImportExportNode(type, info, imports, from, preDots);
    }

    private static int parsePreDots(@NotNull TokenList tokens) {
        int dotCount = 0;
        while (true) {
            if (tokens.tokenIs(TokenType.ELLIPSIS)) {
                dotCount += 3;
                tokens.nextToken();
            } else if (tokens.tokenIs(".")) {
                dotCount++;
                tokens.nextToken();
            } else {
                break;
            }
        }
        return dotCount;
    }

    @Override
    public String toString() {
        String ports = isWildcard ? "*" : TestNode.toString(this.ports);
        String str;
        if (!from.isEmpty()) {
            str = String.format("from %s%s %s %s", ".".repeat(preDots), from, type, ports);
        } else {
            str = type + " " + ports;
        }
        if (as.length == 0) {
            return str;
        } else {
            return str + " as " + TestNode.toString(as);
        }
    }

    public enum Type {
        IMPORT,
        EXPORT,
        TYPEGET,
        ;

        @NotNull
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
