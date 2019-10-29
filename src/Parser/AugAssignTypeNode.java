package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum AugAssignTypeNode {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    FLOOR_DIV("//"),
    POWER("**"),
    LEFT_BITSHIFT("<<"),
    RIGHT_BITSHIFT(">>"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),
    BITWISE_NOT("~"),
    MODULO("%"),
    NULL_COERCE("??"),
    ;

    private static final Map<String, AugAssignTypeNode> values;

    public final String sequence;

    static {
        Map<String, AugAssignTypeNode> temp = new HashMap<>();
        for (AugAssignTypeNode value : AugAssignTypeNode.values()) {
            temp.put(value.sequence, value);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @Contract(pure = true)
    AugAssignTypeNode(String sequence) {
        this.sequence = sequence;
    }

    public static AugAssignTypeNode parse(@NotNull TokenList tokens) {
        AugAssignTypeNode operator = find(tokens.tokenSequence().replaceFirst("=$", ""));
        tokens.nextToken();
        return operator;
    }

    public static AugAssignTypeNode find(String value) {
        if (values.containsKey(value)) {
            return values.get(value);
        } else {
            throw new ParserException("Unknown operator");
        }
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return sequence + '=';
    }
}
