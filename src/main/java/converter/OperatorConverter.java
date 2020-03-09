package main.java.converter;

import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class OperatorConverter implements TestConverter {
    public static final Map<OperatorTypeNode, Bytecode> BYTECODE_MAP;

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
        temp.put(OperatorTypeNode.EQUALS, Bytecode.EQUAL);
        temp.put(OperatorTypeNode.LESS_THAN, Bytecode.LESS_THAN);
        temp.put(OperatorTypeNode.GREATER_THAN, Bytecode.GREATER_THAN);
        temp.put(OperatorTypeNode.LESS_EQUAL, Bytecode.LESS_EQUAL);
        temp.put(OperatorTypeNode.GREATER_EQUAL, Bytecode.GREATER_EQUAL);
        BYTECODE_MAP = Collections.unmodifiableMap(temp);
    }

    private CompilerInfo info;
    private OperatorNode node;
    private int retCount;

    public OperatorConverter(CompilerInfo info, OperatorNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        switch (node.getOperator()) {
            case BOOL_AND:
            case BOOL_NOT:
            case BOOL_OR:
            case BOOL_XOR:
                return new TypeObject[] {Builtins.BOOL};
            case NOT_NULL:
                return notNullReturn();
            case NULL_COERCE:
                return nullCoerceReturn();
        }
        var firstOpConverter = TestConverter.of(info, node.getOperands()[0].getArgument(), 1);
        var retType = firstOpConverter.returnType()[0].operatorReturnType(node.getOperator());
        if (retType == null) {
            throw CompilerInternalError.of("Operator not implemented", node);
        }
        return retType;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        switch (node.getOperator()) {
            case NULL_COERCE:
                return convertNullCoerce(start);
            case BOOL_AND:
            case BOOL_OR:
                return convertBoolOp(start);
            case NOT_NULL:
                return convertNotNull(start);
        }
        if (node.getOperator() == OperatorTypeNode.NULL_COERCE) {
            return convertNullCoerce(start);
        }
        int opCount = node.getOperands().length;
        for (var arg : node.getOperands()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), arg.getArgument(), info, 1));
        }
        var bytecode = BYTECODE_MAP.get(node.getOperator());
        if (opCount == (node.getOperator().isUnary() ? 1 : 2)) {
            bytes.add(bytecode.value);
        } else {
            throw new UnsupportedOperationException("Operators with > 2 operands not yet supported");
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertNullCoerce(int start) {
        assert node.getOperator() == OperatorTypeNode.NULL_COERCE;
        var firstConverter = TestConverter.of(info, node.getOperands()[0].getArgument(), 1);
        if (!firstConverter.returnType()[0].isSuperclass(Builtins.NULL_TYPE)) {  // Non-optional return types won't be null
            var lineInfo = node.getOperands()[0].getLineInfo();
            CompilerWarning.warn("Using ?? operator on non-optional value", lineInfo);
            return firstConverter.convert(start);
        } else if (firstConverter.returnType()[0].equals(Builtins.NULL_TYPE)) {
            var lineInfo = node.getOperands()[0].getLineInfo();
            CompilerWarning.warn("Using ?? operator on value that is always null", lineInfo);
            return TestConverter.bytes(start, node.getOperands()[1].getArgument(), info, 1);
        }
        List<Byte> bytes = new ArrayList<>(firstConverter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.JUMP_NN.value);
        addPostJump(start, bytes);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertBoolOp(int start) {
        assert node.getOperator() == OperatorTypeNode.BOOL_AND || node.getOperator() == OperatorTypeNode.BOOL_OR;
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("true"))));
        bytes.addAll(TestConverter.bytes(start, node.getOperands()[0].getArgument(), info, 1));
        bytes.add(Bytecode.DUP_TOP.value);
        var bytecode = node.getOperator() == OperatorTypeNode.BOOL_OR ? Bytecode.JUMP_FALSE : Bytecode.JUMP_TRUE;
        bytes.add(bytecode.value);
        addPostJump(start, bytes);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private void addPostJump(int start, @NotNull List<Byte> bytes) {
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.POP_TOP.value);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getOperands()[1].getArgument(), info, 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
    }

    @NotNull
    private List<Byte> convertNotNull(int start) {
        assert node.getOperator() == OperatorTypeNode.NOT_NULL;
        var converter = TestConverter.of(info, node.getOperands()[0].getArgument(), 1);
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        if (converter.returnType()[0].equals(Builtins.NULL_TYPE)) {
            throw CompilerException.of(
                    "Cannot use !! operator on variable on variable with type null",
                    node.getOperands()[0]
            );
        } else if (converter.returnType()[0].isSuperclass(Builtins.NULL_TYPE)) {
            bytes.add(Bytecode.DUP_TOP.value);
            bytes.add(Bytecode.JUMP_NN.value);
            int jumpPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
            bytes.add(Bytecode.POP_TOP.value);
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("str"))));  // TODO: Get errors
            bytes.add(Bytecode.LOAD_CONST.value);
            var message = String.format("Value %s asserted non-null, was null", node.getOperands()[0]);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
            bytes.add(Bytecode.THROW_QUICK.value);
            bytes.addAll(Util.shortToBytes((short) 1));
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        } else {
            CompilerWarning.warn("Used !! operator on non-optional value",
                    node.getOperands()[0].getLineInfo());
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private TypeObject[] notNullReturn() {
        var retType = TestConverter.returnType(node.getOperands()[0].getArgument(), info, 1)[0];
        if (retType.equals(Builtins.NULL_TYPE)) {
             // Doesn't particularly matter what, it'll fail later (Maybe return Builtins.THROWS once implemented?)
            return new TypeObject[] {retType};
        } else {
            return new TypeObject[] {retType.stripNull()};
        }
    }

    @NotNull
    public TypeObject[] nullCoerceReturn() {
        var ret0 = TestConverter.returnType(node.getOperands()[0].getArgument(), info, 1)[0];
        var ret1 = TestConverter.returnType(node.getOperands()[1].getArgument(), info, 1)[0];
        var result = ret0.equals(Builtins.NULL_TYPE) ? ret1 : TypeObject.union(ret0.stripNull(), ret1);
        return new TypeObject[] {result};
    }
}
