import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Every type of operator that is valid.
 * @author Patrick Norton
 */
public enum OperatorTypeNode implements AtomicNode {
    ADD("+", Use.TYPICAL),
    R_ADD("r+", Use.R_TYPICAL),
    SUBTRACT("-", Use.TYPICAL, Use.UNARY),
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
    public enum Use {
        UNARY(1),
        REVERSE(1 << 1),
        OPERATOR_SP(1 << 2),
        OP_FUNC(1 << 3),
        STANDARD(1 << 4),
        AUG_ASSIGN(1 << 5),
        TYPICAL(OPERATOR_SP.value | OP_FUNC.value | STANDARD.value | AUG_ASSIGN.value),
        COMPARISON(OPERATOR_SP.value | OP_FUNC.value | STANDARD.value),
        R_TYPICAL(OPERATOR_SP.value | REVERSE.value),
        R_COMPARISON(OPERATOR_SP.value | REVERSE.value),
        U_TYPICAL(TYPICAL.value | UNARY.value),
        BOOLEAN(OP_FUNC.value | STANDARD.value),
        ;
        private final int value;

        @Contract(pure = true)
        Use(int value) {
            this.value = value;
        }
    }

    public final String name;
    private final int usages;

    private static Map<String, OperatorTypeNode> values;

    /**
     * Create new instance of OperatorTypeNode.
     * @param name The sequence of the operator
     */
    @Contract(pure = true)
    OperatorTypeNode(@NotNull String name, @NotNull Use... usages) {
        this.name = name;
        int temp_usages = 0;
        for (Use t : usages) {
            temp_usages |= t.value;
        }
        this.usages = temp_usages;
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
            if ((t.value & this.usages) == 0) {
                return false;
            }
        }
        return true;
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

    /**
     * Parse an OperatorTypeNode from a list of tokens.
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed OperatorTypeNode
     */
    @NotNull
    static OperatorTypeNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR, TokenType.OP_FUNC, TokenType.AUG_ASSIGN);
        Token tok = tokens.getFirst();
        tokens.nextToken();
        switch (tok.token) {
            case OPERATOR:
                return findOp(tok.sequence, Use.STANDARD);
            case OP_FUNC:
                return findOp(tok.sequence.replaceFirst("^\\\\", ""), Use.OP_FUNC);
            case AUG_ASSIGN:
                return findOp(tok.sequence.replaceFirst("=$", ""), Use.AUG_ASSIGN);
            default:
                throw new RuntimeException("Illegal TokenType for OperatorTypeNode.parse "+tok);
        }
    }
}
