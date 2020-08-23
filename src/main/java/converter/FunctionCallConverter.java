package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        var callConverter = TestConverter.of(info, node.getCaller(), 1);
        var retTypes = callConverter.returnType();
        var fnInfo = ensureTypesMatch(retTypes[0]);
        if (isDeterminedFunction(node.getCaller())) {
            return convertOptimized(start, fnInfo);
        }
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        convertArgs(bytes, start, fnInfo);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        for (int i = retCount; i < returnType().length; i++) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private void convertArgs(List<Byte> bytes, int start, @NotNull FunctionInfo fnInfo) {
        var params = node.getParameters();
        var argPositions = fnInfo.getArgs().argPositions(getArgs(params));
        for (var value : params) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), value.getArgument(), info, 1));
            if (value.isVararg()) {
                bytes.add(Bytecode.UNPACK_TUPLE.value);
            }
        }
        var swaps = swapsToOrder(argPositions);
        for (var pair : swaps) {
            bytes.add(Bytecode.SWAP_STACK.value);
            bytes.addAll(Util.shortToBytes((short) (params.length - pair.getKey())));
            bytes.addAll(Util.shortToBytes((short) (params.length - pair.getValue())));
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
            if (cls != null) {  // If the variable is a class, calling it will always return an instance
                return new TypeObject[]{cls.makeMut()};
            }
            var fn = info.fnInfo(name);
            if (fn != null) {
                return fn.getReturns();
            }
            return info.getType(name).operatorReturnType(OpSpTypeNode.CALL, info);
        } else {
            var retType = TestConverter.returnType(node.getCaller(), info, retCount)[0];
            return retType.operatorReturnType(OpSpTypeNode.CALL, info);
        }

    }

    @NotNull
    private FunctionInfo ensureTypesMatch(@NotNull TypeObject callerType) {
        var args = getArgs(node.getParameters());
        var accessLevel = info.accessLevel(callerType);
        var operatorInfo = callerType.operatorInfo(OpSpTypeNode.CALL, accessLevel);
        if (operatorInfo == null) {
            throw CompilerException.format(
                    "Object of type '%s' has no overloaded 'operator ()'",
                    node, callerType.name()
            );
        } else if (!operatorInfo.matches(args)) {
            var argsString = String.join(", ", TypeObject.name(Argument.typesOf(args)));
            var nameArr = TypeObject.name(Argument.typesOf(operatorInfo.getArgs().getNormalArgs()));
            var expectedStr = String.join(", ", nameArr);
            throw CompilerException.format(
                    "Cannot call object of type '%s': arguments given (%s)" +
                            " do not match the arguments of the function (%s)",
                    node, callerType.name(), argsString, expectedStr
            );
        }
        return operatorInfo;
    }

    @NotNull
    private Argument[] getArgs(@NotNull ArgumentNode... args) {
        var result = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var type = TestConverter.returnType(args[i].getArgument(), info, 1)[0];
            result[i] = new Argument(args[i].getVariable().getName(), type, args[i].isVararg(), args[i].getLineInfo());
        }
        return result;
    }

    private boolean isDeterminedFunction(TestNode name) {
        if (!(name instanceof VariableNode)) {
            return false;
        }
        var variableName = (VariableNode) name;
        var strName = variableName.getName();
        return info.fnInfo(strName) != null || (Builtins.BUILTIN_MAP.containsKey(strName)
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
