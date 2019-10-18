package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OpSpTypeNode {
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
    GET_ATTR("[]"),
    SET_ATTR("[]="),
    CALL("()"),
    ITER("iter"),
    NEW("new"),
    IN("in"),
    MISSING("missing"),
    DEL("del"),
    DEL_ATTR("del[]"),
    STR("str"),
    REPR("repr"),
    BOOL("bool"),
    REVERSED("reversed"),
    ;

    private static final Map<String, OpSpTypeNode> values;

    public final String name;

    static {
        Map<String, OpSpTypeNode> temp = new HashMap<>();
        for (OpSpTypeNode value : OpSpTypeNode.values()) {
            temp.put(value.name, value);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @Contract(pure = true)
    OpSpTypeNode(String name) {
        this.name = name;
    }

    public static OpSpTypeNode parse(@NotNull TokenList tokens) {
        OpSpTypeNode operator = find(tokens.getFirst().sequence.replaceFirst("operator *", ""));
        tokens.nextToken();
        return operator;
    }

    public static OpSpTypeNode find(String sequence) {
        if (!values.containsKey(sequence)) {
            throw new ParserException("");
        }
        return values.get(sequence);
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return name;
    }
}
