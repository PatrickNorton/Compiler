package main.java.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum AugAssignTypeNode {
    ADD("+", OperatorTypeNode.ADD),
    SUBTRACT("-", OperatorTypeNode.SUBTRACT),
    MULTIPLY("*", OperatorTypeNode.MULTIPLY),
    DIVIDE("/", OperatorTypeNode.DIVIDE),
    FLOOR_DIV("//", OperatorTypeNode.FLOOR_DIV),
    POWER("**", OperatorTypeNode.POWER),
    LEFT_BITSHIFT("<<", OperatorTypeNode.LEFT_BITSHIFT),
    RIGHT_BITSHIFT(">>", OperatorTypeNode.RIGHT_BITSHIFT),
    BITWISE_AND("&", OperatorTypeNode.BITWISE_AND),
    BITWISE_OR("|", OperatorTypeNode.BITWISE_OR),
    BITWISE_XOR("^", OperatorTypeNode.BITWISE_XOR),
    BITWISE_NOT("~", OperatorTypeNode.BITWISE_NOT),
    MODULO("%", OperatorTypeNode.MODULO),
    NULL_COERCE("??", OperatorTypeNode.NULL_COERCE),
    BOOL_AND("and", OperatorTypeNode.BOOL_AND),
    BOOL_OR("or", OperatorTypeNode.BOOL_OR),
    BOOL_XOR("xor", OperatorTypeNode.BOOL_XOR),
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
    public final OperatorTypeNode operator;

    static {
        Map<String, AugAssignTypeNode> temp = new HashMap<>();
        for (AugAssignTypeNode value : AugAssignTypeNode.values()) {
            temp.put(value.sequence, value);
        }
        values = Collections.unmodifiableMap(temp);
    }

    AugAssignTypeNode(String sequence, OperatorTypeNode op) {
        this.sequence = sequence;
        this.operator = op;
    }

    public static AugAssignTypeNode parse(TokenList tokens) {
        assert tokens.tokenIs(TokenType.AUG_ASSIGN);
        String sequence = tokens.tokenSequence();
        AugAssignTypeNode operator = find(sequence.substring(0, sequence.length() - 1));
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

    public static Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String toString() {
        return sequence + '=';
    }
}
