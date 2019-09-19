package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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

    private static final Map<String, OperatorTypeNode> values;
    private static final LinkedList<OperatorTypeNode[]> operations;

    /**
     * Create new instance of Parser.OperatorTypeNode.
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

    static {  // FIXME? Better way to initialise this?
        operations = new LinkedList<>();
        operations.add(new OperatorTypeNode[]{POWER});
        operations.add(new OperatorTypeNode[]{BITWISE_NOT});
        operations.add(new OperatorTypeNode[]{MULTIPLY, DIVIDE, FLOOR_DIV, MODULO});
        operations.add(new OperatorTypeNode[]{ADD, SUBTRACT});
        operations.add(new OperatorTypeNode[]{LEFT_BITSHIFT, RIGHT_BITSHIFT});
        operations.add(new OperatorTypeNode[]{BITWISE_AND});
        operations.add(new OperatorTypeNode[]{BITWISE_XOR, BITWISE_OR});
        operations.add(new OperatorTypeNode[]{LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL, NOT_EQUALS, EQUALS});
        operations.add(new OperatorTypeNode[]{IN, NOT_IN, IS, IS_NOT});
        operations.add(new OperatorTypeNode[]{BOOL_NOT});
        operations.add(new OperatorTypeNode[]{BOOL_AND});
        operations.add(new OperatorTypeNode[]{BOOL_OR});
        operations.add(new OperatorTypeNode[]{BOOL_XOR});
        operations.add(new OperatorTypeNode[]{CASTED});
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
    static Iterable<OperatorTypeNode[]> orderOfOperations() {
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
