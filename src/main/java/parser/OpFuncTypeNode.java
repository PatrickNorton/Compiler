package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum OpFuncTypeNode {
    ADD("+", OperatorTypeNode.ADD),
    SUBTRACT("-", OperatorTypeNode.SUBTRACT),
    U_SUBTRACT("-", OperatorTypeNode.SUBTRACT),
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
    IN("in", OperatorTypeNode.IN),
    IS("is", OperatorTypeNode.IS),
    NULL_COERCE("??", OperatorTypeNode.NULL_COERCE),
    COMPARE("<=>", OperatorTypeNode.COMPARE),
    ;

    private static final Map<String, OpFuncTypeNode> values;
    public static final Pattern PATTERN = Pattern.compile("^\\\\(" +
            Arrays.stream(values())
                    .filter(x -> x != U_SUBTRACT)
                    .map(o -> o.name)
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(s -> Pattern.compile("\\w$").matcher(s).find() ? s + "\\b" : Pattern.quote(s))
                    .collect(Collectors.joining("|"))
            +")"
    );

    public final String name;
    public final OperatorTypeNode operator;

    static {
        Map<String, OpFuncTypeNode> temp = new HashMap<>();
        for (OpFuncTypeNode value : OpFuncTypeNode.values()) {
            if (value != U_SUBTRACT)
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
