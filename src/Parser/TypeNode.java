package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

// TODO?? Replace with simple DottedVariableNode
public class TypeNode implements AtomicNode {
    private LineInfo lineInfo;
    private DottedVariableNode name;
    private TypeNode[] subtypes;
    private boolean is_vararg;
    private boolean optional;

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name, boolean optional) {
        this(name, new TypeNode[0], false, optional);
    }

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name, TypeNode[] subtypes, boolean is_vararg, boolean optional) {
        this(name.getLineInfo(), name, subtypes, is_vararg, optional);
    }

    @Contract(pure = true)
    public TypeNode(LineInfo lineInfo, DottedVariableNode name, TypeNode[] subtypes, boolean is_vararg, boolean optional) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.subtypes = subtypes;
        this.is_vararg = is_vararg;
        this.optional = optional;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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

    public boolean isOptional() {
        return optional;
    }

    public boolean isDecided() {
        return true;
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
     * @param allowEmpty Whether or not empty varargs are allowed
     * @param isVararg Whether or not the type is a vararg.
     * @return The freshly parsed TypeNode
     */
    @NotNull
    @Contract("_, _, _ -> new")
    private static TypeNode parse(@NotNull TokenList tokens, boolean allowEmpty, boolean isVararg) {
        if (tokens.tokenIs(Keyword.VAR)) {
            return parseVar(tokens);
        }
        DottedVariableNode main;
        if (!tokens.tokenIs(TokenType.NAME)) {
            if (allowEmpty && tokens.tokenIs("[")) {
                main = DottedVariableNode.empty();
            } else {
                throw tokens.error("Expected type name, got " + tokens.getFirst());
            }
        } else {
            main = DottedVariableNode.parseNamesOnly(tokens);
            if (tokens.tokenIs("?")) {
                tokens.nextToken();
                return new TypeNode(main, true);
            }
        }
        if (!tokens.tokenIs("[")) {
            return new TypeNode(main, false);
        }
        tokens.nextToken(true);
        List<TypeNode> subtypes = new ArrayList<>();
        while (!tokens.tokenIs("]")) {
            boolean subclassIsVararg;
            if (tokens.tokenIs("*", "**")) {
                subclassIsVararg = true;
                tokens.nextToken(true);
            } else {
                subclassIsVararg = false;
            }
            subtypes.add(parse(tokens, true, subclassIsVararg));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
                continue;
            }
            tokens.passNewlines();
            if (!tokens.tokenIs("]")) {
                throw tokens.error("Comma must separate subtypes");
            }
        }
        tokens.nextToken();
        boolean optional;
        if (tokens.tokenIs("?")) {
            optional = true;
            tokens.nextToken();
        } else {
            optional = false;
        }
        return new TypeNode(main, subtypes.toArray(new TypeNode[0]), isVararg, optional);
    }

    @NotNull
    static TypeNode[] parseListOnToken(@NotNull TokenList tokens, TokenType sentinel) {
        if (tokens.tokenIs(sentinel)) {
            tokens.nextToken();
            return parseList(tokens);
        } else {
            return new TypeNode[0];
        }
    }

    @NotNull
    static TypeNode[] parseListOnToken(@NotNull TokenList tokens, Keyword sentinel) {
        if (tokens.tokenIs(sentinel)) {
            tokens.nextToken();
            return parseList(tokens);
        } else {
            return new TypeNode[0];
        }
    }

    @NotNull
    static TypeNode[] parseList(@NotNull TokenList tokens) {
        List<TypeNode> types = new ArrayList<>();
        while (tokens.tokenIs(TokenType.NAME, Keyword.VAR)) {
            types.add(parse(tokens));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken();
        }
        return types.toArray(new TypeNode[0]);
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
        return parseListOnToken(tokens, TokenType.ARROW);
    }

    @NotNull
    private static TypeNode parseVar(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.VAR);
        tokens.nextToken();
        return new TypeNode(DottedVariableNode.empty(), false) {
            @Override
            public boolean isDecided() {
                return false;
            }

            @Override
            public String toString() {
                return "var";
            }
        };
    }

    public static String returnString(@NotNull TypeNode... values) {
        if (values.length == 0) {
            return "";
        }
        StringJoiner sj = new StringJoiner(", ", " -> ", "");
        for (TypeNode t : values) {
            sj.add(t.toString());
        }
        return sj.toString();
    }

    @Override
    public String toString() {
        if (subtypes.length > 0) {
            String subtypes = TestNode.toString(this.subtypes);
            return (is_vararg ? "*" : "") + name + "[" + subtypes + "]" + (optional ? "?" : "");
        }
        return (is_vararg ? "*" : "") + name + (optional ? "?" : "");
    }
}
