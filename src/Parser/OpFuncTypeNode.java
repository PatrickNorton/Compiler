package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum OpFuncTypeNode {
    ADD("+", OperatorTypeNode.ADD),
    SUBTRACT("-", OperatorTypeNode.SUBTRACT),
    U_SUBTRACT("u-", OperatorTypeNode.SUBTRACT),
    MULTIPLY("*", OperatorTypeNode.MULTIPLY),
    DIVIDE("/", OperatorTypeNode.DIVIDE),
    FLOOR_DIV("//", OperatorTypeNode.FLOOR_DIV),
    POWER("**", OperatorTypeNode.POWER),
    EQUALS("==", OperatorTypeNode.EQUALS),
    NOT_EQUALS("!=", OperatorTypeNode.NOT_EQUALS),
    GREATER_THAN(">", OperatorTypeNode.GREATER_THAN),
    LESS_THAN("<", OperatorTypeNode.LESS_THAN),
    GREATER_EQUAL(">=", OperatorTypeNode.GREATER_EQUAL),
    LESS_EQUAL("<=", OperatorTypeNode.LESS_EQUAL),
    LEFT_BITSHIFT("<<", OperatorTypeNode.LEFT_BITSHIFT),
    RIGHT_BITSHIFT(">>", OperatorTypeNode.RIGHT_BITSHIFT),
    BITWISE_AND("&", OperatorTypeNode.BITWISE_AND),
    BITWISE_OR("|", OperatorTypeNode.BITWISE_OR),
    BITWISE_XOR("^", OperatorTypeNode.BITWISE_XOR),
    BITWISE_NOT("~", OperatorTypeNode.BITWISE_NOT),
    MODULO("%", OperatorTypeNode.MODULO),
    BOOL_AND("and", OperatorTypeNode.BOOL_AND),
    BOOL_OR("or", OperatorTypeNode.BOOL_OR),
    BOOL_NOT("not", OperatorTypeNode.BOOL_NOT),
    BOOL_XOR("xor", OperatorTypeNode.BOOL_XOR),
    ;

    private static final Map<String, OpFuncTypeNode> values;
    public static final Pattern PATTERN = Pattern.compile("^\\\\(" +
            Arrays.stream(values())
                    .map((OpFuncTypeNode o) -> o.name)
                    .sorted((String i, String j) -> j.length() - i.length())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"))
            +")(\\b|(?<!\\w))"
    );

    public final String name;
    public final OperatorTypeNode operator;

    static {
        Map<String, OpFuncTypeNode> temp = new HashMap<>();
        for (OpFuncTypeNode value : OpFuncTypeNode.values()) {
            temp.put(value.name, value);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @Contract(pure = true)
    OpFuncTypeNode(String name, OperatorTypeNode operator) {
        this.name = name;
        this.operator = operator;
    }

    public static OpFuncTypeNode parse(@NotNull TokenList tokens) {
        OpFuncTypeNode operator = find(tokens.tokenSequence().replaceFirst("^\\\\", ""));
        tokens.nextToken();
        return operator;
    }

    public static OpFuncTypeNode find(String value) {
        if (values.containsKey(value)) {
            return values.get(value);
        } else {
            throw new ParserException("Unknown operator");
        }
    }

    @Contract(pure = true)
    static Pattern pattern() {
        return PATTERN;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return "\\" + name;
    }
}
