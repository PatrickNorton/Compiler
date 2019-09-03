import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class TypeNode implements AtomicNode {
    private DottedVariableNode name;
    private TypeNode[] subtypes;
    private boolean is_vararg;

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name) {
        this.name = name;
        this.subtypes = new TypeNode[0];
    }

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name, TypeNode[] subtypes, boolean is_vararg) {
        this.name = name;
        this.subtypes = subtypes;
        this.is_vararg = is_vararg;
    }

    public DottedVariableNode getName() {
        return name;
    }

    public TypeNode[] getSubtypes() {
        return subtypes;
    }

    public boolean getIs_vararg() {
        return is_vararg;
    }

    /**
     * Parse a TypeNode from a list of tokens.
     * <p>
     *     The syntax for a TypeNode is: <code>{@link DottedVariableNode} ["["
     *     [(["*"] {@link TypeNode} | "[" [["*"] {@link TypeNode}]
     *     *("," ["*"] {@link TypeNode}) [","]] "]") *(","
     *     (["*"] {@link TypeNode} | "[" [["*"] {@link TypeNode}]
     *     *("," ["*"] {@link TypeNode}) [","] "]") [","] "]"]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The parsed TypeNode
     */
    @NotNull
    static TypeNode parse(TokenList tokens) {
        return parse(tokens, false, false);
    }

    /**
     * Parse a TypeNode from a list of tokens.
     * @param tokens The list of tokens to destructively parse.
     * @param allow_empty Whether or not empty varargs are allowed
     * @param is_vararg Whether or not the type is a vararg.
     * @return The freshly parsed TypeNode
     */
    @NotNull
    @Contract("_, _, _ -> new")
    static TypeNode parse(@NotNull TokenList tokens, boolean allow_empty, boolean is_vararg) {
        DottedVariableNode main;
        if (!tokens.tokenIs(TokenType.NAME)) {
            if (allow_empty && tokens.tokenIs("[")) {
                main = new DottedVariableNode();
            } else {
                throw new ParserException("Expected type name, got " + tokens.getFirst());
            }
        } else {
            main = DottedVariableNode.parse(tokens);
        }
        if (!tokens.tokenIs("[")) {
            return new TypeNode(main);
        }
        tokens.nextToken(true);
        LinkedList<TypeNode> subtypes = new LinkedList<>();
        while (!tokens.tokenIs("]")) {
            boolean subcls_vararg;
            if (tokens.tokenIs("*", "**")) {
                subcls_vararg = true;
                tokens.nextToken(true);
            } else {
                subcls_vararg = false;
            }
            subtypes.add(parse(tokens, true, subcls_vararg));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
                continue;
            }
            tokens.passNewlines();
            if (!tokens.tokenIs("]")) {
                throw new ParserException("Comma must separate subtypes");
            }
        }
        tokens.nextToken();
        return new TypeNode(main, subtypes.toArray(new TypeNode[0]), is_vararg);
    }

    /**
     * Parse the return value of some function.
     * <p>
     *     The syntax for a return value indicator is: <code>"->" {@link
     *     TypeNode} *("," {@link TypeNode}) [","]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The list of return types for the function being parsed
     */
    @NotNull
    static TypeNode[] parseRetVal(@NotNull TokenList tokens) {
        if (tokens.tokenIs("{")) {
            return new TypeNode[0];
        }
        if (!tokens.tokenIs(TokenType.ARROW)) {
            throw new ParserException("Return value must use arrow operator");
        }
        tokens.nextToken();
        LinkedList<TypeNode> types = new LinkedList<>();
        while (!tokens.tokenIs("{") && !tokens.tokenIs(TokenType.NEWLINE)) {
            types.add(TypeNode.parse(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken();
            }
        }
        return types.toArray(new TypeNode[0]);
    }
}
