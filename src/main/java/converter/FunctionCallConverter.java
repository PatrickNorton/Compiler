package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.EscapedOperatorNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class FunctionCallConverter implements TestConverter {
    private final CompilerInfo info;
    private final FunctionCallNode node;
    private final int retCount;

    public FunctionCallConverter(CompilerInfo info, FunctionCallNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (node.getCaller() instanceof EscapedOperatorNode) {
            return convertOp(start);
        }
        var callConverter = TestConverter.of(info, node.getCaller(), 1);
        var retTypes = callConverter.returnType();
        var fnInfo = ensureTypesMatch(retTypes[0]);
        if (isDeterminedFunction(node.getCaller())) {
            return convertOptimized(start, fnInfo);
        }
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        int argc = convertArgs(bytes, start, fnInfo);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) argc));
        var retType = returnType();
        for (int i = retCount; i < retType.length; i++) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private List<Byte> convertOp(int start) {
        assert node.getCaller() instanceof EscapedOperatorNode;
        List<Byte> bytes = new ArrayList<>();
        var caller = (EscapedOperatorNode) node.getCaller();
        if (caller.getOperator().operator == OperatorTypeNode.IS) {
            return new IsConverter(true, node.getParameters(), node, info, retCount).convert(start);
        }
        var op = OpSpTypeNode.translate(caller.getOperator().operator);
        var first = firstArgType();
        ensureTypesMatch(op, first, getArgsExceptFirst(node.getParameters()));
        var opInfo = first.tryOperatorInfo(node, op, info);
        int argc = convertOpArgs(bytes, start, opInfo);
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) op.ordinal()));
        bytes.addAll(Util.shortToBytes((short) argc));
        return bytes;
    }

    private TypeObject firstArgType() {
        var param = node.getParameters()[0];
        var retType = TestConverter.returnType(param.getArgument(), info, 1)[0];
        if (param.isVararg()) {
            return retType.getGenerics().get(0);
        } else {
            return retType;
        }
    }

    private int convertArgs(List<Byte> bytes, int start, @NotNull FunctionInfo fnInfo) {
        var params = node.getParameters();
        var argPositions = fnInfo.getArgs().argPositions(getArgs(params));
        return convertInnerArgs(bytes, start, params, argPositions);
    }

    private int convertOpArgs(List<Byte> bytes, int start, FunctionInfo fnInfo) {
        var params = node.getParameters();
        var argPositions = fnInfo.getArgs().argPositions(getArgsExceptFirst(params));
        return convertInnerArgs(bytes, start, params, argPositions) - 1;
    }

    private int convertInnerArgs(List<Byte> bytes, int start, ArgumentNode[] params, @NotNull int[] argPositions) {
        var argc = params.length;
        for (var value : params) {
            var converter = TestConverter.of(info, value.getArgument(), 1);
            bytes.addAll(converter.convert(start + bytes.size()));
            if (value.isVararg()) {
                var retType = converter.returnType()[0];
                if (!(retType instanceof TupleType)) {
                    throw CompilerException.format(
                            "Illegal parameter expansion: Value must be a tuple, instead '%s'",
                            value, retType.name()
                    );
                }
                bytes.add(Bytecode.UNPACK_TUPLE.value);
                argc += ((TupleType) retType).getGenerics().size() - 1;
            }
        }
        var swaps = swapsToOrder(argPositions);
        for (var pair : swaps) {
            var dist1 = (short) (params.length - pair.getKey() - 1);
            var dist2 = (short) (params.length - pair.getValue() - 1);
            addSwap(bytes, dist1, dist2);
        }
        return argc;
    }

    private void addSwap(List<Byte> bytes, short dist1, short dist2) {
        assert dist1 != dist2;
        var d1 = (short) Math.min(dist1, dist2);
        var d2 = (short) Math.max(dist1, dist2);
        if (d1 == 0 && d2 == 1) {
            bytes.add(Bytecode.SWAP_2.value);
        } else {
            bytes.add(Bytecode.SWAP_STACK.value);
            bytes.addAll(Util.shortToBytes(d1));
            bytes.addAll(Util.shortToBytes(d2));
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        if (node.getCaller() instanceof VariableNode) {
            var name = node.getVariable().getName();
            if (info.varIsUndefined(name)) {
                throw CompilerException.format("Undefined variable '%s'", node, name);
            }
            var cls = info.classOf(name);
            if (cls.isPresent()) {  // If the variable is a class, calling it will always return an instance
                return new TypeObject[]{cls.orElseThrow().makeMut()};
            }
            var fn = info.fnInfo(name);
            if (fn.isPresent()) {
                return fn.orElseThrow().getReturns();
            }
        }
        var retType = TestConverter.returnType(node.getCaller(), info, retCount)[0];
        var retInfo = retType.tryOperatorInfo(node, OpSpTypeNode.CALL, info);
        var generics = retInfo.generifyArgs(getArgs(node.getParameters()));
        if (generics.isEmpty()) {
            return retInfo.getReturns();
        } else {
            var cls = retInfo.toCallable();
            var returns = retInfo.getReturns();
            var gen = turnMapToList(generics.orElseThrow());
            if (returns.length < retCount) {
                throw CompilerInternalError.format(
                        "Length %d less than length %d", node, returns.length, retCount
                );
            }
            TypeObject[] result = new TypeObject[retCount];
            for (int i = 0; i < retCount; i++) {
                result[i] = returns[i].generifyWith(cls, gen);
            }
            return result;
        }
    }

    @NotNull
    private static List<TypeObject> turnMapToList(@NotNull Map<Integer, TypeObject> values) {
        List<TypeObject> result = new ArrayList<>();
        for (var pair : values.entrySet()) {
            int index = pair.getKey();
            var value = pair.getValue();
            if (index == result.size()) {
                result.add(value);
            } else if (index < result.size()) {
                assert result.get(index) == null;
                result.set(index, value);
            } else {
                while (result.size() < index) {
                    result.add(null);
                }
                result.add(value);
            }
        }
        assert !result.contains(null);
        return result;
    }

    @NotNull
    private FunctionInfo ensureTypesMatch(@NotNull TypeObject callerType) {
        return ensureTypesMatch(OpSpTypeNode.CALL, callerType, getArgs(node.getParameters()));
    }

    private FunctionInfo ensureTypesMatch(OpSpTypeNode operator, TypeObject callerType, Argument[] args) {
        var accessLevel = info.accessLevel(callerType);
        var operatorInfo = callerType.operatorInfo(operator, accessLevel);
        if (operatorInfo.isEmpty()) {
            throw CompilerException.format(
                    "Object of type '%s' has no overloaded 'operator ()'",
                    node, callerType.name()
            );
        } else {
            var opInfo = operatorInfo.orElseThrow();
            var opGenerics = opInfo.generifyArgs(args);
            if (opGenerics.isEmpty()) {
                var argsString = String.join(", ", TypeObject.name(Argument.typesOf(args)));
                var nameArr = TypeObject.name(Argument.typesOf(opInfo.getArgs().getNormalArgs()));
                var expectedStr = String.join(", ", nameArr);
                throw CompilerException.format(
                        "Cannot call object of type '%s': arguments given (%s)" +
                                " do not match the arguments of the function (%s)",
                        node, callerType.name(), argsString, expectedStr
                );
            }
            return opInfo;
        }
    }

    @NotNull
    private Argument[] getArgs(@NotNull ArgumentNode... args) {
        var result = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            var type = TestConverter.returnType(arg.getArgument(), info, 1)[0];
            var lineInfo = arg.getArgument().getLineInfo();
            result[i] = new Argument(arg.getVariable().getName(), type, arg.isVararg(), lineInfo);
        }
        return result;
    }

    private Argument[] getArgsExceptFirst(ArgumentNode... args) {
        if (args[0].isVararg()) {
            var result = getArgs(args);
            var res0 = result[0];
            assert res0.getType() instanceof TupleType;
            var generics = res0.getType().getGenerics();
            var newGen = generics.subList(0, generics.size() - 1).toArray(new TypeObject[0]);
            var resType = Builtins.TUPLE.generify(newGen);
            result[0] = new Argument(res0.getName(), resType, res0.isVararg(), res0.getLineInfo());
        return result;
        } else {
            return getArgs(Arrays.copyOfRange(args, 1, args.length));
        }
    }

    private boolean isDeterminedFunction(TestNode name) {
        if (!(name instanceof VariableNode)) {
            return false;
        }
        var variableName = (VariableNode) name;
        var strName = variableName.getName();
        return info.fnInfo(strName).isPresent() || (Builtins.BUILTIN_MAP.containsKey(strName)
                && (BUILTINS_TO_OPERATORS.containsKey(strName) || strName.equals("type")));
    }

    @NotNull
    private List<Byte> convertOptimized(int start, FunctionInfo fnInfo) {
        assert node.getCaller() instanceof VariableNode;
        var strName = ((VariableNode) node.getCaller()).getName();
        if (Builtins.BUILTIN_MAP.containsKey(strName)) {
            return convertBuiltin(start, strName);
        } else {
            return convertCallFn(start, strName, fnInfo);
        }
    }

    private static final Map<String, OpSpTypeNode> BUILTINS_TO_OPERATORS = Map.of(
            "int", OpSpTypeNode.INT,
            "str", OpSpTypeNode.STR,
            "bool", OpSpTypeNode.BOOL,
            "repr", OpSpTypeNode.REPR,
            "iter", OpSpTypeNode.ITER
    );

    @NotNull
    private List<Byte> convertBuiltin(int start, String strName) {
        assert Builtins.BUILTIN_MAP.containsKey(strName);
        List<Byte> bytes = new ArrayList<>();
        if (BUILTINS_TO_OPERATORS.containsKey(strName)) {
            var params = node.getParameters();
            assert params.length == 1;  // No operator needs more than self
            bytes.addAll(TestConverter.bytes(start + bytes.size(), params[0].getArgument(), info, 1));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) BUILTINS_TO_OPERATORS.get(strName).ordinal()));
            bytes.addAll(Util.shortZeroBytes());
            return bytes;
        } else if (strName.equals("type")) {
            var params = node.getParameters();
            assert params.length == 1;
            var converter = TestConverter.of(info, params[0].getArgument(), 1);
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.GET_TYPE.value);
            return bytes;
        } else {
            throw new RuntimeException();
        }

    }

    @NotNull
    private List<Byte> convertCallFn(int start, String strName, FunctionInfo fnInfo) {
        var fnIndex = info.fnIndex(strName);
        assert fnIndex != -1;
        List<Byte> bytes = new ArrayList<>();
        convertArgs(bytes, start, fnInfo);
        bytes.add(Bytecode.CALL_FN.value);
        bytes.addAll(Util.shortToBytes(fnIndex));
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        return bytes;
    }

    /**
     * Calculates the sequence of swaps in order to make {@code values} be in
     * strictly ascending order.
     * <p>
     *     This assumes that {@code values} contains all ints from {@code 0} to
     *     {@code values.length - 1}.
     * </p>
     *
     * @param values The values to figure out how to sort
     * @return The pairs of values to swap
     */
    @NotNull
    private List<Pair<Integer, Integer>> swapsToOrder(@NotNull int... values) {
        List<Pair<Integer, Integer>> swaps = new ArrayList<>();
        int[] currentState = values.clone();
        for (int i = 0; i < values.length; i++) {
            int ptr = i;
            while (currentState[ptr] != ptr) {
                swaps.add(Pair.of(currentState[ptr], ptr));
                arrSwap(currentState, currentState[ptr], ptr);
                ptr = currentState[ptr];
            }
        }
        return swaps;
    }

    private static void arrSwap(@NotNull int[] values, int a, int b) {
        int temp = values[a];
        values[a] = values[b];
        values[b] = temp;
    }
}
