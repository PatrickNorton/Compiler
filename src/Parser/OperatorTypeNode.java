package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every type of operator that is valid.
 * @author Patrick Norton
 */
public enum OperatorTypeNode implements AtomicNode {
    ADD("+", Use.TYPICAL),
    R_ADD("r+", Use.R_TYPICAL),
    SUBTRACT("-", Use.U_TYPICAL),
    R_SUBTRACT("r-", Use.R_TYPICAL),
    UNARY_MINUS("u-", Use.U_TYPICAL),
    MULTIPLY("*", Use.TYPICAL),
    R_MULTIPLY("r*", Use.R_TYPICAL),
    DIVIDE("/", Use.TYPICAL),
    R_DIVIDE("r/", Use.R_TYPICAL),
    FLOOR_DIV("//", Use.TYPICAL),
    R_FLOOR_DIV("r//", Use.R_TYPICAL),
    POWER("**", Use.TYPICAL),
    R_POWER("r**", Use.R_TYPICAL),
    EQUALS("==", Use.COMPARISON),
    R_EQUALS("r==", Use.R_COMPARISON),
    NOT_EQUALS("!=", Use.COMPARISON),
    R_NOT_EQUALS("r!=", Use.R_COMPARISON),
    GREATER_THAN(">", Use.COMPARISON),
    R_GREATER_THAN("r>", Use.R_COMPARISON),
    LESS_THAN("<", Use.COMPARISON),
    R_LESS_THAN("r<", Use.R_COMPARISON),
    GREATER_EQUAL(">=", Use.COMPARISON),
    R_GREATER_EQUAL("r>=", Use.R_COMPARISON),
    LESS_EQUAL("<=", Use.COMPARISON),
    R_LESS_EQUAL("r<=", Use.R_COMPARISON),
    LEFT_BITSHIFT("<<", Use.TYPICAL),
    R_LEFT_BITSHIFT("r<<", Use.R_TYPICAL),
    RIGHT_BITSHIFT(">>", Use.TYPICAL),
    R_RIGHT_BITSHIFT("r>>", Use.R_TYPICAL),
    BITWISE_AND("&", Use.TYPICAL),
    R_BITWISE_AND("r&", Use.R_TYPICAL),
    BITWISE_OR("|", Use.TYPICAL),
    R_BITWISE_OR("r|", Use.R_TYPICAL),
    BITWISE_XOR("^", Use.TYPICAL),
    R_BITWISE_XOR("r^", Use.R_TYPICAL),
    BITWISE_NOT("~", Use.U_TYPICAL),
    MODULO("%", Use.TYPICAL),
    R_MODULO("r%", Use.R_TYPICAL),
    BOOL_AND("and", Use.BOOLEAN),
    BOOL_OR("or", Use.BOOLEAN),
    BOOL_NOT("not", Use.BOOLEAN, Use.UNARY),
    BOOL_XOR("xor", Use.BOOLEAN),
    GET_ATTR("[]", Use.OPERATOR_SP),
    SET_ATTR("[]=", Use.OPERATOR_SP),
    CALL("()", Use.OPERATOR_SP),
    ITER("iter", Use.OPERATOR_SP),
    NEW("new", Use.OPERATOR_SP),
    IN("in", Use.STANDARD, Use.OPERATOR_SP),
    NOT_IN("not in", Use.STANDARD),
    MISSING("missing", Use.OPERATOR_SP),
    DEL("del", Use.OPERATOR_SP),
    DEL_ATTR("del[]", Use.OPERATOR_SP),
    STR("str", Use.OPERATOR_SP),
    REPR("repr", Use.OPERATOR_SP),
    BOOL("bool", Use.OPERATOR_SP),
    CASTED("casted", Use.STANDARD),
    IS("is", Use.STANDARD),
    IS_NOT("is not", Use.STANDARD),
    ;

    /**
     * The different usage types for the operator.
     */
    private enum Use {
        UNARY,
        REVERSE,
        OPERATOR_SP,
        OP_FUNC,
        STANDARD,
        AUG_ASSIGN,
        TYPICAL(OPERATOR_SP, OP_FUNC, STANDARD, AUG_ASSIGN),
        COMPARISON(OPERATOR_SP, OP_FUNC, STANDARD),
        R_TYPICAL(OPERATOR_SP, REVERSE),
        R_COMPARISON(OPERATOR_SP, REVERSE),
        U_TYPICAL(OPERATOR_SP, OP_FUNC, STANDARD, AUG_ASSIGN, UNARY),
        BOOLEAN(OP_FUNC, STANDARD),
        ;
        private final Use[] tempSet;
        private EnumSet<Use> set;

        @Contract(pure = true)
        Use() {
            this.tempSet = new Use[] {this};
        }

        @Contract(pure = true)
        Use(Use... values) {
            this.tempSet = values;
        }

        static {
            for (Use u : Use.values()) {
                u.set = EnumSet.of(u.tempSet[0], u.tempSet);
            }
        }
    }

    public final String name;
    private final EnumSet<Use> usages;

    private static final Map<String, OperatorTypeNode> values;
    private static final List<EnumSet<OperatorTypeNode>> operations = List.of(
            EnumSet.of(POWER),
            EnumSet.of(BITWISE_NOT),
            EnumSet.of(MULTIPLY, DIVIDE, FLOOR_DIV, MODULO),
            EnumSet.of(ADD, SUBTRACT),
            EnumSet.of(LEFT_BITSHIFT, RIGHT_BITSHIFT),
            EnumSet.of(BITWISE_AND),
            EnumSet.of(BITWISE_XOR, BITWISE_OR),
            EnumSet.of(LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL, NOT_EQUALS, EQUALS),
            EnumSet.of(IN, NOT_IN, IS, IS_NOT),
            EnumSet.of(BOOL_NOT),
            EnumSet.of(BOOL_AND),
            EnumSet.of(BOOL_OR),
            EnumSet.of(BOOL_XOR),
            EnumSet.of(CASTED)
    );

    /**
     * Create new instance of Parser.OperatorTypeNode.
     * @param name The sequence of the operator
     */
    @Contract(pure = true)
    OperatorTypeNode(@NotNull String name, @NotNull Use... usages) {
        this.name = name;
        this.usages = EnumSet.noneOf(Use.class);
        for (Use use : usages) {
            this.usages.addAll(use.set);
        }
    }

    static {  // Initialise the map
        Map<String, OperatorTypeNode> temp = new HashMap<>();
        for (OperatorTypeNode op : OperatorTypeNode.values()) {
            temp.put(op.name, op);
        }
        values = Collections.unmodifiableMap(temp);
    }

    /**
     * Check if the operator is a valid use thereof
     * @param usages The usages to test for
     * @return Whether or not it is a valid use
     */
    @Contract(pure = true)
    public boolean isUse(@NotNull Use... usages) {
        for (Use t : usages) {
            if (!this.usages.containsAll(t.set)) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public boolean isUnary() {
        return this.isUse(Use.UNARY);
    }

    /**
     * Find an operator in the enum.
     * @param name The sequence of the operator
     * @return The actual operator enum
     */
    static OperatorTypeNode findOp(@NotNull String name) {
        if (name.contains("  ")) {
            name = name.replaceAll(" +", " ");
        }
        if (values.containsKey(name)) {
            return values.get(name);
        } else {
            throw new ParserException("Unknown operator");
        }
    }

    /**
     * Find an operator in the enum, ensuring it corresponds to the types given.
     * @param name The sequence of the operator
     * @param usage The types to check for compliance
     * @return The actual operator enum
     */
    @NotNull
    static OperatorTypeNode findOp(@NotNull String name, Use... usage) {
        OperatorTypeNode op = findOp(name);
        if (op.isUse(usage)) {
            return op;
        } else {
            throw new ParserException("Illegal operator "+op.name);
        }
    }

    @Contract(pure = true)
    static Iterable<EnumSet<OperatorTypeNode>> orderOfOperations() {
        return operations;
    }

    /**
     * Parse an Parser.OperatorTypeNode from a list of tokens.
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed Parser.OperatorTypeNode
     */
    @NotNull
    static OperatorTypeNode parse(@NotNull TokenList tokens) {
        OperatorTypeNode op = fromToken(tokens.getFirst());
        tokens.nextToken();
        return op;
    }

    @NotNull
    static OperatorTypeNode fromToken(@NotNull Token token) {
        assert token.is(TokenType.OPERATOR, TokenType.KEYWORD, TokenType.BOOL_OP,
                TokenType.OP_FUNC, TokenType.AUG_ASSIGN, TokenType.OPERATOR_SP);
        switch (token.token) {
            case KEYWORD:
            case BOOL_OP:
            case OPERATOR:
                return findOp(token.sequence, Use.STANDARD);
            case OP_FUNC:
                return findOp(token.sequence.replaceFirst("^\\\\", ""), Use.OP_FUNC);
            case AUG_ASSIGN:
                return findOp(token.sequence.replaceFirst("=$", ""), Use.AUG_ASSIGN);
            case OPERATOR_SP:
                return findOp(token.sequence.replaceFirst("operator *", ""), Use.OPERATOR_SP);
            default:
                throw new RuntimeException("Illegal Parser.TokenType for Parser.OperatorTypeNode.parse "+token);
        }
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return name;
    }
}
