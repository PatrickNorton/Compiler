import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Every type of operator that is valid.
 * @author Patrick Norton
 */
public enum OperatorTypeNode implements AtomicNode {
    ADD("+"),
    R_ADD("r+"),
    SUBTRACT("-"),
    R_SUBTRACT("r-"),
    UNARY_MINUS("u-"),
    MULTIPLY("*"),
    R_MULTIPLY("r*"),
    DIVIDE("/"),
    R_DIVIDE("r/"),
    FLOOR_DIV("//"),
    R_FLOOR_DIV("r//"),
    POWER("**"),
    R_POWER("r**"),
    EQUALS("=="),
    R_EQUALS("r=="),
    NOT_EQUALS("!="),
    R_NOT_EQUALS("r!="),
    GREATER_THAN(">"),
    R_GREATER_THAN("r>"),
    LESS_THAN("<"),
    R_LESS_THAN("r<"),
    GREATER_EQUAL(">="),
    R_GREATER_EQUAL("r>="),
    LESS_EQUAL("<="),
    R_LESS_EQUAL("r<="),
    LEFT_BITSHIFT("<<"),
    R_LEFT_BITSHIFT("r<<"),
    RIGHT_BITSHIFT(">>"),
    R_RIGHT_BITSHIFT("r>>"),
    BITWISE_AND("&"),
    R_BITWISE_AND("r&"),
    BITWISE_OR("|"),
    R_BITWISE_OR("r|"),
    BITWISE_XOR("^"),
    R_BITWISE_XOR("r^"),
    BITWISE_NOT("~"),
    MODULO("%"),
    R_MODULO("r%"),
    BOOL_AND("and"),
    BOOL_OR("or"),
    BOOL_NOT("not"),
    BOOL_XOR("xor"),
    GET_ATTR("[]"),
    SET_ATTR("[]="),
    CALL("()"),
    ITER("iter"),
    NEW("new"),
    IN("in"),
    NOT_IN("not in"),
    MISSING("missing"),
    DEL("del"),
    DEL_ATTR("del[]"),
    STR("str"),
    REPR("repr"),
    BOOL("bool"),
    CASTED("casted"),
    IS("is"),
    IS_NOT("is not"),
    ;
    public final String name;

    private static Map<String, OperatorTypeNode> values;

    /**
     * Create new instance of OperatorTypeNode.
     * @param name The sequence of the operator
     */
    OperatorTypeNode(String name) {
        this.name = name;
    }

    static {  // Initialise the map
        Map<String, OperatorTypeNode> temp = new HashMap<>();
        for (OperatorTypeNode op : OperatorTypeNode.values()) {
            temp.put(op.name, op);
        }
        values = Collections.unmodifiableMap(temp);
    }

    /**
     * Find an operator in the enum.
     * @param name The sequence of the operator
     * @return The actual operator enum
     */
    static OperatorTypeNode find_op(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        } else {
            throw new ParserException("Unknown operator");
        }
    }
}
