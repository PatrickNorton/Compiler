package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

/**
 * The class representing a typeget statement.
 * @author Patrick Norton
 * @see ImportStatementNode
 * @see ExportStatementNode
 */
public class TypegetStatementNode implements ImportExportNode {
    private DottedVariableNode[] typegets;
    private DottedVariableNode from;
    private DottedVariableNode[] as;

    @Contract(pure = true)
    public TypegetStatementNode(DottedVariableNode[] imports, DottedVariableNode from) {
        this(imports, from, new DottedVariableNode[0]);
    }

    @Contract(pure = true)
    public TypegetStatementNode(DottedVariableNode[] imports, DottedVariableNode from, DottedVariableNode[] as) {
        this.typegets = imports;
        this.from = from;
        this.as = as;
    }

    public DottedVariableNode[] getTypegets() {
        return typegets;
    }

    public DottedVariableNode getFrom() {
        return from;
    }

    public DottedVariableNode[] getAs() {
        return as;
    }

    /**
     * Parse a Parser.TypegetStatementNode from a list of tokens.
     * <p>
     *     The syntax for a typeget statement is: <code>["from" {@link
     *     DottedVariableNode}] "typeget" {@link DottedVariableNode} *(","
     *     {@link DottedVariableNode}) [","]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed Parser.TypegetStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static TypegetStatementNode parse(@NotNull TokenList tokens) {
        DottedVariableNode from = DottedVariableNode.empty();
        if (tokens.tokenIs("from")) {
            tokens.nextToken();
            from = DottedVariableNode.parseName(tokens);
        }
        assert tokens.tokenIs("typeget");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = DottedVariableNode.parseList(tokens, false);
        if (tokens.tokenIs("as")) {
            tokens.nextToken();
            DottedVariableNode[] as = DottedVariableNode.parseList(tokens, false);
            return new TypegetStatementNode(typegets, from, as);
        }
        return new TypegetStatementNode(typegets, from);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (DottedVariableNode d : typegets) {
            sj.add(d.toString());
        }
        if (!from.isEmpty()) {
            return "from " + from + " typeget " + sj;
        } else {
            return "typeget " + sj;
        }
    }
}
