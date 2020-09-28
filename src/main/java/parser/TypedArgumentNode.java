package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class representing a typed argument.
 * @author Patrick Norton
 * @see TypedArgumentListNode
 */
public class TypedArgumentNode implements BaseNode {
    private LineInfo lineInfo;
    private TypeLikeNode type;
    private VariableNode name;
    private TestNode defaultVal;
    private boolean isVararg;
    private String varargType;

    public TypedArgumentNode(TypeLikeNode type, VariableNode name, TestNode defaultVal, boolean isVararg, String varargType) {
        this(type.getLineInfo(), type, name, defaultVal, isVararg, varargType);
    }
    /**
     * Construct a new instance of TypedArgumentListNode.
     * @param type The type of the argument
     * @param name The name of the argument
     * @param defaultVal The default value of the argument
     * @param isVararg If the argument has a vararg
     * @param varargType The type of the vararg, if it exists
     */
    @Contract(pure = true)
    public TypedArgumentNode(LineInfo lineInfo, TypeLikeNode type, VariableNode name, TestNode defaultVal,
                             boolean isVararg, String varargType) {
        this.lineInfo = lineInfo;
        this.type = type;
        this.name = name;
        this.defaultVal = defaultVal;
        this.isVararg = isVararg;
        this.varargType = varargType;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeLikeNode getType() {
        return type;
    }

    public VariableNode getName() {
        return name;
    }

    public TestNode getDefaultVal() {
        return defaultVal;
    }

    public boolean getVararg() {
        return isVararg;
    }

    public String getVarargType() {
        return varargType;
    }

    /**
     * Parse a TypedArgumentNode from a list of tokens, typed if stated so.
     * <p>
     *     The syntax for a typed argument is: <code>["*" | "**"] {@link
     *     TypeNode} {@link VariableNode} ["=" {@link TestNode}]</code>.
     *     The syntax for a untyped argument is: <code>["*" | "**"] {@link
     *     VariableNode} ["=" {@link TestNode}]</code>.
     * </p>
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param isTyped Whether or not the node is typed
     * @return The freshly parsed node
     */
    @NotNull
    @Contract("_, _ -> new")
    static TypedArgumentNode parse(@NotNull TokenList tokens, boolean isTyped) {
        String varargType;
        if (tokens.tokenIs("*", "**")) {
            varargType = tokens.tokenSequence();
            tokens.nextToken(true);
        } else {
            varargType = "";
        }
        return parse(tokens, isTyped, varargType);
    }

    /**
     * Parse the second half of a TypedArgumentNode, with the vararg type
     * already given.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param isTyped If the node is typed
     * @param varargType The type of the vararg ({@code ""} if no vararg)
     * @return The freshly parsed TypedArgumentNode
     */
    @Contract("_, _, _ -> new")
    @NotNull
    private static TypedArgumentNode parse(@NotNull TokenList tokens, boolean isTyped, String varargType) {
        if (tokens.tokenIs(Keyword.VAR)) {
            throw tokens.error("var is not allowed in a typed argument");
        }
        TypeLikeNode type = isTyped ? TypeLikeNode.parse(tokens, true) : TypeNode.var();
        VariableNode var = VariableNode.parse(tokens);
        tokens.passNewlines();
        TestNode defaultValue = TestNode.parseOnToken(tokens, "=", true);
        return new TypedArgumentNode(type, var, defaultValue, !varargType.isEmpty(), varargType);
    }

    /**
     * Parse a TypedArgumentNode, allowing the possibility of the token being
     * the vararg-only marker ({@code "*"}). Returns {@code null} if that is
     * the case.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param allowUntyped Whether or not to allow untyped nodes
     * @param typeDecided Whether or not it is known if the node is typed
     * @return The freshly parsed TypedArgumentNode, or null
     */
    @Nullable
    static TypedArgumentNode parseAllowingEmpty(@NotNull TokenList tokens, boolean allowUntyped, boolean typeDecided) {
        String varargType;
        tokens.passNewlines();
        if (tokens.tokenIs("*", "**")) {
            varargType = tokens.tokenSequence();
            tokens.nextToken(true);
            if (varargType.equals("*") && !TestNode.nextIsTest(tokens)) {
                return null;
            }
        } else {
            varargType = "";
        }
        if (!typeDecided) {
            allowUntyped = argumentIsUntyped(tokens);
        }
        return parse(tokens, !allowUntyped, varargType);
    }

    /**
     * Whether or not the TypedArgumentNode is typed or untyped.
     *
     * @param tokens The list of tokens to use to make a decision
     * @return Whether or not the node is untyped
     */
    static boolean argumentIsUntyped(@NotNull TokenList tokens) {
        int size = TypeLikeNode.sizeOfType(tokens, tokens.tokenIs("*", "**") ? 1 : 0);
        return size == 0 || !tokens.tokenIs(size + tokens.numberOfNewlines(size), TokenType.NAME);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s%s", varargType, type, name, defaultVal.isEmpty() ? "" : defaultVal);
    }
}
