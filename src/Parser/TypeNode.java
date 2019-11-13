package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO?? Replace with simple DottedVariableNode
public class TypeNode implements TypeLikeNode {
    private static final Set<String> TYPE_NODE_POSSIBLE = Set.of("[", "*");

    private LineInfo lineInfo;
    private DottedVariableNode name;
    private TypeLikeNode[] subtypes;
    private boolean isVararg;
    private boolean optional;

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name, boolean optional) {
        this(name, new TypeNode[0], false, optional);
    }

    @Contract(pure = true)
    public TypeNode(DottedVariableNode name, TypeLikeNode[] subtypes, boolean isVararg, boolean optional) {
        this(name.getLineInfo(), name, subtypes, isVararg, optional);
    }

    @Contract(pure = true)
    public TypeNode(LineInfo lineInfo, DottedVariableNode name, TypeLikeNode[] subtypes, boolean isVararg, boolean optional) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.subtypes = subtypes;
        this.isVararg = isVararg;
        this.optional = optional;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public DottedVariableNode getName() {
        return name;
    }

    @Override
    public TypeLikeNode[] getSubtypes() {
        return subtypes;
    }

    @Override
    public boolean isVararg() {
        return isVararg;
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
    private static TypeNode parseVar(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.VAR);
        tokens.nextToken();
        return var();
    }

    static boolean nextIsType(@NotNull TokenList tokens) {
        return tokens.tokenIs(TokenType.NAME, Keyword.VAR) || TYPE_NODE_POSSIBLE.contains(tokens.tokenSequence());
    }

    /**
     * Return a new TypeNode representing the keyword {@link Keyword#VAR var}.
     *
     * @return The new TypeNode
     */
    @NotNull
    @Contract(" -> new")
    public static TypeNode var() {
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

    @Override
    public String toString() {
        if (name.isEmpty() && subtypes.length == 0) {
            return isVararg ? "*[]" : "[]";
        }
        if (subtypes.length > 0) {
            String subtypes = TestNode.toString(this.subtypes);
            return (isVararg ? "*" : "") + name + "[" + subtypes + "]" + (optional ? "?" : "");
        }
        return (isVararg ? "*" : "") + name + (optional ? "?" : "");
    }
}
