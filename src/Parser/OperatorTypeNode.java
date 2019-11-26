package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Every type of operator that is valid.
 * @author Patrick Norton
 */
public enum OperatorTypeNode implements AtomicNode {
    ADD("+", 3),
    SUBTRACT("-", 3),
    U_SUBTRACT("u-", 1, true),
    MULTIPLY("*", 2),
    DIVIDE("/", 2),
    FLOOR_DIV("//", 2),
    POWER("**", 0),
    EQUALS("==", 7),
    NOT_EQUALS("!=", 7),
    GREATER_THAN(">", 7),
    LESS_THAN("<", 7),
    GREATER_EQUAL(">=", 7),
    LESS_EQUAL("<=", 7),
    LEFT_BITSHIFT("<<", 4),
    RIGHT_BITSHIFT(">>", 4),
    BITWISE_AND("&", 5),
    BITWISE_OR("|", 6),
    BITWISE_XOR("^", 6),
    BITWISE_NOT("~", 1, true),
    MODULO("%", 2),
    BOOL_AND("and", 11),
    BOOL_OR("or", 12),
    BOOL_NOT("not", 10, true),
    BOOL_XOR("xor", 13),
    IN("in", 8),
    NOT_IN("not in", 8),
    CASTED("casted", 14),
    IS("is", 8),
    IS_NOT("is not", 8),
    NULL_COERCE("??", 0),
    NOT_NULL("!!", 0, true, true),
    OPTIONAL("?", 0, true, true),
    INSTANCEOF("instanceof", 9),
    NOT_INSTANCEOF("not instanceof", 9),
    COMPARE("<=>", 7),
    ;

    public static final Pattern PATTERN = Pattern.compile("^(" +
            Arrays.stream(values())
                    .filter(t -> t != U_SUBTRACT)
                    .map(Object::toString)
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(s -> Pattern.compile("\\w$").matcher(s).find() ? s + "\\b" : Pattern.quote(s))
                    .map(s -> s.replaceFirst(" ", " +"))
                    .collect(Collectors.joining("|"))
            + ")"
    );

    public final String name;
    public final int precedence;
    private final boolean unary;
    private final boolean postfix;

    private static final Map<String, OperatorTypeNode> values;

    @Contract(pure = true)
    OperatorTypeNode(String name, int precedence) {
        this(name, precedence, false);
    }

    @Contract(pure = true)
    OperatorTypeNode(String name, int precedence, boolean unary) {
        this(name, precedence, unary, false);
    }

    @Contract(pure = true)
    OperatorTypeNode(String name, int precedence, boolean unary, boolean postfix) {
        this.name = name;
        this.precedence = precedence;
        this.unary = unary;
        this.postfix = postfix;
    }

    static {  // Initialise the map
        Map<String, OperatorTypeNode> temp = new HashMap<>();
        for (OperatorTypeNode op : OperatorTypeNode.values()) {
            temp.put(op.name, op);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    /**
     * Whether or not the operator type is unary.
     * @return If the operator is unary
     */
    @Contract(pure = true)
    public boolean isUnary() {
        return this.unary;
    }

    @Contract(pure = true)
    public boolean isPostfix() {
        return postfix;
    }

    /**
     * Find an operator in the enum.
     * @param name The sequence of the operator
     * @return The actual operator enum
     */
    static OperatorTypeNode findOp(@NotNull String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        } else {
            throw new ParserException("Unknown operator");
        }
    }

    /**
     * Parse an OperatorTypeNode from a list of tokens.
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed OperatorTypeNode
     */
    @NotNull
    static OperatorTypeNode parse(@NotNull TokenList tokens) {
        OperatorTypeNode op = fromToken(tokens.getFirst());
        tokens.nextToken();
        return op;
    }

    /**
     * Parse an OperatorTypeNode from a token.
     * @param token The token from which to get the operator type
     * @return The OperatorTypeNode found.
     */
    @NotNull
    static OperatorTypeNode fromToken(@NotNull Token token) {
        assert token.is(TokenType.OPERATOR, TokenType.KEYWORD);
        return findOp(token.sequence);
    }

    @Contract(pure = true)
    static Pattern pattern() {
        return PATTERN;
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return name;
    }
}
