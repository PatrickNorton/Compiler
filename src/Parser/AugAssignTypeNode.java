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
    BOOL_AND("and"),
    BOOL_OR("or"),
    BOOL_XOR("xor"),
    ;

    private static final Map<String, AugAssignTypeNode> values;
    public static final Pattern PATTERN = Pattern.compile("^(" +
            Arrays.stream(values())
                    .map(Object::toString)
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"))
            + ")"
    );

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

    @Contract(pure = true)
    public static Pattern pattern() {
        return PATTERN;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return sequence + '=';
    }
}
