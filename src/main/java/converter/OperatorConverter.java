package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class OperatorConverter implements TestConverter {
    public static final Map<OperatorTypeNode, Bytecode> BYTECODE_MAP;
    protected static final Set<OperatorTypeNode> MANDATORY_ARG_COUNT;

    static {  // TODO: Make these members of OperatorTypeNode
        var temp = new EnumMap<OperatorTypeNode, Bytecode>(OperatorTypeNode.class);
        temp.put(OperatorTypeNode.ADD, Bytecode.PLUS);
        temp.put(OperatorTypeNode.SUBTRACT, Bytecode.MINUS);
        temp.put(OperatorTypeNode.MULTIPLY, Bytecode.TIMES);
        temp.put(OperatorTypeNode.DIVIDE, Bytecode.DIVIDE);
        temp.put(OperatorTypeNode.FLOOR_DIV, Bytecode.FLOOR_DIV);
        temp.put(OperatorTypeNode.MODULO, Bytecode.MOD);
        temp.put(OperatorTypeNode.POWER, Bytecode.POWER);
        temp.put(OperatorTypeNode.LEFT_BITSHIFT, Bytecode.L_BITSHIFT);
        temp.put(OperatorTypeNode.RIGHT_BITSHIFT, Bytecode.R_BITSHIFT);
        temp.put(OperatorTypeNode.BITWISE_AND, Bytecode.BITWISE_AND);
        temp.put(OperatorTypeNode.BITWISE_OR, Bytecode.BITWISE_OR);
        temp.put(OperatorTypeNode.BITWISE_XOR, Bytecode.BITWISE_XOR);
        temp.put(OperatorTypeNode.COMPARE, Bytecode.COMPARE);
        temp.put(OperatorTypeNode.U_SUBTRACT, Bytecode.U_MINUS);
        temp.put(OperatorTypeNode.BITWISE_NOT, Bytecode.BITWISE_NOT);
        temp.put(OperatorTypeNode.BOOL_AND, Bytecode.BOOL_AND);
        temp.put(OperatorTypeNode.BOOL_OR, Bytecode.BOOL_OR);
        temp.put(OperatorTypeNode.BOOL_NOT, Bytecode.BOOL_NOT);
        temp.put(OperatorTypeNode.IS, Bytecode.IDENTICAL);
        temp.put(OperatorTypeNode.INSTANCEOF, Bytecode.INSTANCEOF);
        temp.put(OperatorTypeNode.EQUALS, Bytecode.EQUAL);
        temp.put(OperatorTypeNode.LESS_THAN, Bytecode.LESS_THAN);
        temp.put(OperatorTypeNode.GREATER_THAN, Bytecode.GREATER_THAN);
        temp.put(OperatorTypeNode.LESS_EQUAL, Bytecode.LESS_EQUAL);
        temp.put(OperatorTypeNode.GREATER_EQUAL, Bytecode.GREATER_EQUAL);
        BYTECODE_MAP = Collections.unmodifiableMap(temp);
    }

    static {
        MANDATORY_ARG_COUNT = EnumSet.of(
                OperatorTypeNode.BOOL_NOT,
                OperatorTypeNode.IS,
                OperatorTypeNode.IS_NOT,
                OperatorTypeNode.IN,
                OperatorTypeNode.NOT_IN,
                OperatorTypeNode.CASTED,
                OperatorTypeNode.U_SUBTRACT
        );
    }

    public static OperatorConverter of(CompilerInfo info, OperatorNode node, int retCount) {
        switch (node.getOperator()) {
            case NULL_COERCE:
            case NOT_NULL:
            case OPTIONAL:
                return new NullOpConverter(node.getOperator(), node.getOperands(), node, info, retCount);
            case BOOL_AND:
            case BOOL_OR:
            case BOOL_NOT:
            case BOOL_XOR:
                return new BoolOpConverter(node.getOperator(), node.getOperands(), node, info, retCount);
            case IS:
                return new IsConverter(true, node.getOperands(), node, info, retCount);
            case IS_NOT:
                return new IsConverter(false, node.getOperands(), node, info, retCount);
            case IN:
                return new InConverter(true, node.getOperands(), node, info, retCount);
            case NOT_IN:
                return new InConverter(false, node.getOperands(), node, info, retCount);
            case INSTANCEOF:
                return new InstanceConverter(true, node.getOperands(), node, info, retCount);
            case NOT_INSTANCEOF:
                return new InstanceConverter(false, node.getOperands(), node, info, retCount);
            default:
                return new NormalOperatorConverter(node.getOperator(), node.getOperands(), node, info, retCount);
        }
    }

    public static Pair<List<Byte>, TypeObject> convertWithAs(int start, OperatorNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).convertWithAs(start);
    }

    @NotNull
    protected abstract Pair<List<Byte>, TypeObject> convertWithAs(int start);

    protected static CompilerException asException(Lined lineInfo) {
        return CompilerException.of(
                "Cannot use 'as' here, condition must be an " +
                        "'instanceof', '?', or 'is not null' statement", lineInfo
        );
    }
}
