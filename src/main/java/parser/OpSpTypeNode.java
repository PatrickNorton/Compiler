package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    GET_SLICE("[:]"),
    SET_ATTR("[]="),
    SET_SLICE("[:]="),
    CALL("()"),
    COMPARE("<=>"),
    R_COMPARE("r<=>"),
    ITER("iter"),
    ITER_SLICE("iter[:]"),
    NEW("new"),
    IN("in"),
    MISSING("missing"),
    DEL("del"),
    DEL_ATTR("del[]"),
    DEL_SLICE("del[:]"),
    STR("str"),
    REPR("repr"),
    BOOL("bool"),
    INT("int"),
    REVERSED("reversed"),
    HASH("hash"),
    ENTER("enter"),
    EXIT("exit"),
    ;

    private static final Map<String, OpSpTypeNode> values;
    public static final Pattern PATTERN = Pattern.compile("^operator\\b *(" +
             Arrays.stream(values())
                     .map(o -> o.name)
                     .sorted(Comparator.comparingInt(String::length).reversed())
                     .map(s -> Pattern.compile("\\w$").matcher(s).find() ? s + "\\b" : Pattern.quote(s))
                     .collect(Collectors.joining("|"))
            + ")"
    );

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
        OpSpTypeNode operator = find(tokens.tokenSequence().replaceFirst("operator *", ""));
        tokens.nextToken();
        return operator;
    }

    public static OpSpTypeNode find(String sequence) {
        if (!values.containsKey(sequence)) {
            throw new ParserException("");
        }
        return values.get(sequence);
    }

    private static final Map<OperatorTypeNode, OpSpTypeNode> OP_SP_TRANSLATION_MAP;

    static {
        var temp = new EnumMap<OperatorTypeNode, OpSpTypeNode>(OperatorTypeNode.class);
        temp.put(OperatorTypeNode.ADD, OpSpTypeNode.ADD);
        temp.put(OperatorTypeNode.SUBTRACT, OpSpTypeNode.SUBTRACT);
        temp.put(OperatorTypeNode.U_SUBTRACT, OpSpTypeNode.UNARY_MINUS);
        temp.put(OperatorTypeNode.MULTIPLY, OpSpTypeNode.MULTIPLY);
        temp.put(OperatorTypeNode.DIVIDE, OpSpTypeNode.DIVIDE);
        temp.put(OperatorTypeNode.FLOOR_DIV, OpSpTypeNode.FLOOR_DIV);
        temp.put(OperatorTypeNode.POWER, OpSpTypeNode.POWER);
        temp.put(OperatorTypeNode.EQUALS, OpSpTypeNode.EQUALS);
        temp.put(OperatorTypeNode.NOT_EQUALS, OpSpTypeNode.NOT_EQUALS);
        temp.put(OperatorTypeNode.GREATER_THAN, OpSpTypeNode.GREATER_THAN);
        temp.put(OperatorTypeNode.LESS_THAN, OpSpTypeNode.LESS_THAN);
        temp.put(OperatorTypeNode.GREATER_EQUAL, OpSpTypeNode.GREATER_EQUAL);
        temp.put(OperatorTypeNode.LESS_EQUAL, OpSpTypeNode.LESS_EQUAL);
        temp.put(OperatorTypeNode.LEFT_BITSHIFT, OpSpTypeNode.LEFT_BITSHIFT);
        temp.put(OperatorTypeNode.RIGHT_BITSHIFT, OpSpTypeNode.RIGHT_BITSHIFT);
        temp.put(OperatorTypeNode.BITWISE_AND, OpSpTypeNode.BITWISE_AND);
        temp.put(OperatorTypeNode.BITWISE_OR, OpSpTypeNode.BITWISE_OR);
        temp.put(OperatorTypeNode.BITWISE_XOR, OpSpTypeNode.BITWISE_XOR);
        temp.put(OperatorTypeNode.BITWISE_NOT, OpSpTypeNode.BITWISE_NOT);
        temp.put(OperatorTypeNode.MODULO, OpSpTypeNode.MODULO);
        temp.put(OperatorTypeNode.IN, OpSpTypeNode.IN);
        temp.put(OperatorTypeNode.COMPARE, OpSpTypeNode.COMPARE);
        OP_SP_TRANSLATION_MAP = Collections.unmodifiableMap(temp);
    }

    public static OpSpTypeNode translate(OperatorTypeNode node) {
        return OP_SP_TRANSLATION_MAP.get(node);
    }

    @Contract(pure = true)
    static Pattern pattern() {
        return PATTERN;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return "operator " + name;
    }
}
