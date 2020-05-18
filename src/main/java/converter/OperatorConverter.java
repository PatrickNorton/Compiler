package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
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

    private final CompilerInfo info;
    private final OperatorNode node;
    private final int retCount;

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
            case IN:
            case IS:
            case IS_NOT:
                return new TypeObject[] {Builtins.BOOL};
            case NOT_NULL:
                return notNullReturn();
            case NULL_COERCE:
                return nullCoerceReturn();
            case NOT_EQUALS:
                return notEqualsReturn();
        }
        var firstOpConverter = TestConverter.of(info, node.getOperands()[0].getArgument(), 1);
        var retType = firstOpConverter.returnType()[0].operatorReturnType(node.getOperator(), info);
        return retType == null ? new TypeObject[] {Builtins.THROWS} : retType;
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
            case NOT_EQUALS:
                return convertNotEquals(start);
            case IN:
                return convertContains(start);
            case IS:
            case IS_NOT:
                return convertIs(start);
        }
        int opCount = node.getOperands().length;
        TypeObject opType = null;
        for (var arg : node.getOperands()) {
            var converter = TestConverter.of(info, arg.getArgument(), 1);
            var retTypes = converter.returnType();
            if (retTypes.length == 0) {
                throw CompilerException.of("Cannot use return type of function with 0 returns", arg);
            }
            var retType = retTypes[0];
            if (opType != null && opType.operatorReturnType(node.getOperator(), info) == null) {
                throw CompilerException.format(
                        "'%s' returns type '%s', which has no overloaded '%s'",
                        arg, arg, opType.name(), node.getOperator()
                );
            }
            opType = opType == null ? retType : opType.operatorReturnType(node.getOperator(), info)[0];
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

    private Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        switch (node.getOperator()) {  // TODO: not (x is null)
            case IS_NOT:
                return convertIsNot(start);
            case INSTANCEOF:
                return convertInstanceof(start);
            default:
                throw CompilerException.of(
                        "Cannot use 'as' here, condition must be an " +
                                "'instanceof' or 'is not null' statement", node
                );
        }
    }

    @NotNull
    private Pair<List<Byte>, TypeObject> convertIsNot(int start) {
        assert node.getOperands().length == 2;
        var arg0 = node.getOperands()[0].getArgument();
        var arg1 = node.getOperands()[1].getArgument();
        if (!(arg1 instanceof VariableNode) || !((VariableNode) arg1).getName().equals("null")) {
            throw CompilerException.of(
                    "Cannot use 'as' here, 'is not' comparison must be done to null",
                    arg1
            );
        }
        var condType = TestConverter.returnType(arg0, info, 1)[0];
        if (!condType.isSuperclass(Builtins.NULL_TYPE)) {
            CompilerWarning.warn("Using 'is not null' comparison on non-nullable variable", arg0);
        } else if (condType.equals(Builtins.NULL_TYPE)) {
            CompilerWarning.warn("Using 'is null' comparison on variable that must be null", arg0);
        }
        var asType = condType.stripNull();
        var bytes = new ArrayList<>(TestConverter.bytes(start, arg0, info, 1));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), arg1, info, 1));
        bytes.add(Bytecode.IDENTICAL.value);
        bytes.add(Bytecode.BOOL_NOT.value);
        return Pair.of(bytes, asType);
    }

    @NotNull
    private Pair<List<Byte>, TypeObject> convertInstanceof(int start) {
        assert node.getOperands().length == 2;
        var arg0 = node.getOperands()[0].getArgument();
        var arg1 = node.getOperands()[1].getArgument();
        var converter1 = TestConverter.of(info, arg1, 1);
        var arg1ret = converter1.returnType()[0];
        if (!Builtins.TYPE.isSuperclass(arg1ret)) {
            throw CompilerException.of(
                    "'instanceof' operator requires return type that is an instance of 'type'", arg1
            );
        }
        var instanceType = arg1ret.operatorReturnType(OpSpTypeNode.CALL, info)[0]; // calling a type will always return an instance
        var bytes = new ArrayList<>(TestConverter.bytes(start, arg0, info, 1));
        bytes.addAll(converter1.convert(start + bytes.size()));
        bytes.add(Bytecode.INSTANCEOF.value);
        return Pair.of(bytes, instanceType);
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
    private List<Byte> convertNotEquals(int start) {
        int opCount = node.getOperands().length;
        assert opCount == 2 && node.getOperator() == OperatorTypeNode.NOT_EQUALS;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getOperands()[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getOperands()[1].getArgument(), info, 1));
        if (opCount == (node.getOperator().isUnary() ? 1 : 2)) {
            bytes.add(Bytecode.EQUAL.value);
        } else {
            throw new UnsupportedOperationException("Operators with > 2 operands not yet supported");
        }
        bytes.add(Bytecode.BOOL_NOT.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertContains(int start) {
        assert node.getOperands().length == 2 && node.getOperator() == OperatorTypeNode.IN;
        var operands = node.getOperands();
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), operands[1].getArgument(), info, 1));
        bytes.add(Bytecode.SWAP_2.value);
        bytes.add(Bytecode.CONTAINS.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertIs(int start) {
        assert node.getOperator() == OperatorTypeNode.IS || node.getOperator() == OperatorTypeNode.IS_NOT;
        if (node.getOperands().length == 2) {
            List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getOperands()[0].getArgument(), info, 1));
            bytes.addAll(TestConverter.bytes(start, node.getOperands()[1].getArgument(), info, 1));
            bytes.add(Bytecode.IDENTICAL.value);
            if (node.getOperator() == OperatorTypeNode.IS_NOT) {
                bytes.add(Bytecode.BOOL_NOT.value);
            }
            return bytes;
        } else {
            throw new UnsupportedOperationException("'is' with more than 2 operands not yet supported");
        }
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
    private TypeObject[] nullCoerceReturn() {
        var ret0 = TestConverter.returnType(node.getOperands()[0].getArgument(), info, 1)[0];
        var ret1 = TestConverter.returnType(node.getOperands()[1].getArgument(), info, 1)[0];
        var result = ret0.equals(Builtins.NULL_TYPE) ? ret1 : TypeObject.union(ret0.stripNull(), ret1);
        return new TypeObject[] {result};
    }

    @NotNull
    private TypeObject[] notEqualsReturn() {
        var firstOpConverter = TestConverter.of(info, node.getOperands()[0].getArgument(), 1);
        var retType = firstOpConverter.returnType()[0].operatorReturnType(node.getOperator(), info);
        if (retType == null) {
            throw CompilerInternalError.of("Operator != not implemented", node);
        }
        return retType;
    }

    public static Pair<List<Byte>, TypeObject> convertWithAs(int start, OperatorNode node, CompilerInfo info, int retCount) {
        return new OperatorConverter(info, node, retCount).convertWithAs(start);
    }
}
