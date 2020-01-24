package main.java.converter;

import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OperatorConverter implements TestConverter {
    private static final Map<OperatorTypeNode, Bytecode> BYTECODE_MAP;

    static {  // TODO: Make these members of OperatorTypeNode
        var temp = new EnumMap<OperatorTypeNode, Bytecode>(OperatorTypeNode.class);
        temp.put(OperatorTypeNode.ADD, Bytecode.PLUS);
        temp.put(OperatorTypeNode.SUBTRACT, Bytecode.MINUS);
        temp.put(OperatorTypeNode.MULTIPLY, Bytecode.TIMES);
        temp.put(OperatorTypeNode.DIVIDE, Bytecode.DIVIDE);
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
        BYTECODE_MAP = Collections.unmodifiableMap(temp);
    }

    private CompilerInfo info;
    private OperatorNode node;

    public OperatorConverter(CompilerInfo info, OperatorNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public TypeObject returnType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        int opCount = node.getOperands().length;
        for (var arg : node.getOperands()) {
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), arg.getArgument(), info));
        }
        var bytecode = BYTECODE_MAP.get(node.getOperator());
        if (opCount == (node.getOperator().isUnary() ? 1 : 2)) {
            bytes.add(bytecode.value);
        } else {
            throw new UnsupportedOperationException("Operators with > 2 operands not yet supported");
        }
        return bytes;
    }
}