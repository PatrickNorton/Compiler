package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.EscapedOperatorNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FunctionCallConverter implements TestConverter {
    private final CompilerInfo info;
    private final FunctionCallNode node;
    private final int retCount;

    public FunctionCallConverter(CompilerInfo info, FunctionCallNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        if (node.getCaller() instanceof EscapedOperatorNode) {
            var op = ((EscapedOperatorNode) node.getCaller()).getOperator().operator;
            var conv = OperatorConverter.ofComponents(info, op, node.getParameters(), node, retCount);
            return conv.constantReturn();
        } else if (isDeterminedFunction(node.getCaller())) {
            return determinedFunctionConstant();
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        return convert(start, false);
    }

    public List<Byte> convertTail(int start) {
        return convert(start, true);
    }

    private List<Byte> convert(int start, boolean tail) {
        if (node.getCaller() instanceof EscapedOperatorNode) {
            return convertOp(start);
        }
        var callConverter = TestConverter.of(info, node.getCaller(), 1);
        var retTypes = callConverter.returnType();
        var fnPair = ensureTypesMatch(retTypes[0]);
        var fnInfo = fnPair.getKey();
        var needsMakeOption = fnPair.getValue();
        if (isDeterminedFunction(node.getCaller())) {
            return convertOptimized(start, fnInfo, needsMakeOption, tail);
        }
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        int argc = convertArgs(bytes, start, fnInfo, needsMakeOption);
        bytes.add(tail ? Bytecode.TAIL_TOS.value : Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) argc));
        var retType = returnType();
        for (int i = retCount; i < retType.length; i++) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private List<Byte> convertOp(int start) {
        assert node.getCaller() instanceof EscapedOperatorNode;
        var op = ((EscapedOperatorNode) node.getCaller()).getOperator().operator;
        var conv = OperatorConverter.ofComponents(info, op, node.getParameters(), node, retCount);
        return conv.convert(start);
    }

    private int convertArgs(List<Byte> bytes, int start, @NotNull FunctionInfo fnInfo, Set<Integer> needsMakeOption) {
        var params = node.getParameters();
        var argPositions = fnInfo.getArgs().argPositions(getArgs(params));
        return convertInnerArgs(info, bytes, start, params, argPositions, needsMakeOption);
    }

    private static int convertInnerArgs(
            CompilerInfo info, List<Byte> bytes, int start, ArgumentNode[] params,
            int[] argPositions, Set<Integer> needsMakeOption
    ) {
        int argc = 0;
        for (int i = 0; i < params.length; i++) {
            var value = params[i];
            var converter = TestConverter.of(info, value.getArgument(), 1);
            bytes.addAll(converter.convert(start + bytes.size()));
            if (value.isVararg()) {
                var retType = converter.returnType()[0];
                if (!(retType instanceof TupleType)) {
                    if (retType.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                        throw CompilerTodoError.of("Unpacking iterables in function calls", value);
                    } else {
                        throw CompilerException.format(
                                "Illegal parameter expansion: Value must be a tuple, instead '%s'",
                                value, retType.name()
                        );
                    }
                }
                bytes.add(Bytecode.UNPACK_TUPLE.value);
                var genCount = ((TupleType) retType).getGenerics().size();
                for (int j = 0; j < genCount; j++) {
                    if (needsMakeOption.contains(i + argc + j)) {
                        if (genCount - j - 1 != 0) {
                            addSwap(bytes, (short) 0, (short) (genCount - j - 1));
                            bytes.add(Bytecode.MAKE_OPTION.value);
                            addSwap(bytes, (short) 0, (short) (genCount - j - 1));
                        } else {
                            bytes.add(Bytecode.MAKE_OPTION.value);
                        }
                    }
                }
                argc += genCount - 1;
            } else if (needsMakeOption.contains(i + argc)) {
                bytes.add(Bytecode.MAKE_OPTION.value);
            }
        }
        var swaps = swapsToOrder(argPositions);
        for (var pair : swaps) {
            var dist1 = (short) (params.length - pair.getKey() - 1);
            var dist2 = (short) (params.length - pair.getValue() - 1);
            addSwap(bytes, dist1, dist2);
        }
        return argc + params.length;
    }

    private static void addSwap(List<Byte> bytes, short dist1, short dist2) {
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
        if (node.getCaller() instanceof EscapedOperatorNode) {
            return escapedOpReturn();
        }
        var retType = TestConverter.returnType(node.getCaller(), info, retCount)[0];
        var retInfo = retType.tryOperatorInfo(node, OpSpTypeNode.CALL, info);
        return generifyReturns(retInfo);
    }

    private TypeObject[] generifyReturns(FunctionInfo fnInfo) {
        return generifyReturns(fnInfo, info, node.getParameters(), node, retCount);
    }

    public static TypeObject[] generifyReturns(
            FunctionInfo fnInfo, CompilerInfo info, ArgumentNode[] params, Lined node, int retCount
    ) {
        var args = getArgs(info, params);
        var genPair = fnInfo.generifyArgs(args)
                .orElseThrow(() -> argError(node, fnInfo.toCallable().name(), args, fnInfo.getArgs().getNormalArgs()));
        var generics = genPair.getKey();
        if (generics.isEmpty()) {
            var returns = fnInfo.getReturns();
            if (returns.length < retCount) {
                throw CompilerException.format(
                    "Cannot call function %s with %d returns: only returns %d",
                        node, fnInfo.getName(), retCount, returns.length
                );
            }
            return returns;
        }
        var cls = fnInfo.toCallable();
        var returns = fnInfo.getReturns();
        var gen = turnMapToList(generics);
        if (returns.length < retCount) {
            throw CompilerInternalError.format(
                    "Length %d less than length %d", node, returns.length, retCount
            );
        }
        TypeObject[] result = new TypeObject[retCount == -1 ? returns.length : retCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = returns[i].generifyWith(cls, gen);
        }
        return result;
    }

    private TypeObject[] escapedOpReturn() {
        assert node.getCaller() instanceof EscapedOperatorNode;
        var escapedOp = (EscapedOperatorNode) node.getCaller();
        var op = escapedOp.getOperator().operator;
        var converter = OperatorConverter.ofComponents(info, op, node.getParameters(), node, retCount);
        return converter.returnType();
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

    private Pair<FunctionInfo, Set<Integer>> ensureTypesMatch(TypeObject callerType) {
        return ensureTypesMatch(info, node, callerType, node.getParameters());
    }

    private static Pair<FunctionInfo, Set<Integer>> ensureTypesMatch(
            CompilerInfo info, Lined lineInfo, TypeObject callerType, ArgumentNode... arguments
    ) {
        var args = getArgs(info, arguments);
        var operatorInfo = callerType.tryOperatorInfo(lineInfo, OpSpTypeNode.CALL, info);
        var opGenerics = operatorInfo.generifyArgs(args);
        if (opGenerics.isEmpty()) {
            throw argError(lineInfo, callerType.name(), args, operatorInfo.getArgs().getNormalArgs());
        }
        return Pair.of(operatorInfo, opGenerics.orElseThrow().getValue());
    }

    private static CompilerException argError(Lined node, String name, Argument[] args, Argument[] expected) {
        var argsString = String.join(", ", TypeObject.name(Argument.typesOf(args)));
        var nameArr = TypeObject.name(Argument.typesOf(expected));
        var expectedStr = String.join(", ", nameArr);
        return CompilerException.format(
                "Cannot call object of type '%s': arguments given" +
                        " do not match the arguments of the function%n" +
                        "Arguments received: %s%nArguments expected: %s",
                node, name, argsString, expectedStr
        );
    }

    @NotNull
    private Argument[] getArgs(@NotNull ArgumentNode... args) {
        return getArgs(info, args);
    }

    @NotNull
    private static Argument[] getArgs(CompilerInfo info, @NotNull ArgumentNode... args) {
        var result = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            var type = TestConverter.returnType(arg.getArgument(), info, 1)[0];
            var lineInfo = arg.getArgument().getLineInfo();
            result[i] = new Argument(arg.getVariable().getName(), type, arg.isVararg(), lineInfo);
        }
        return result;
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
    private List<Byte> convertOptimized(int start, FunctionInfo fnInfo, Set<Integer> needsMakeOption, boolean tail) {
        assert node.getCaller() instanceof VariableNode;
        var strName = ((VariableNode) node.getCaller()).getName();
        if (Builtins.BUILTIN_MAP.containsKey(strName)) {
            return convertBuiltin(start, strName);
        } else {
            return convertCallFn(start, strName, fnInfo, needsMakeOption, tail);
        }
    }

    private Optional<LangConstant> determinedFunctionConstant() {
        assert isDeterminedFunction(node.getCaller());
        assert node.getCaller() instanceof VariableNode;
        if (node.getParameters().length != 1 || !node.getParameters()[0].getVararg().isEmpty()) {
            return Optional.empty();
        }
        var strName = ((VariableNode) node.getCaller()).getName();
        var op = BUILTINS_TO_OPERATORS.get(strName);
        if (op != null) {
            var arg = node.getParameters()[0].getArgument();
            var constantReturn = TestConverter.constantReturn(arg, info, 1);
            if (constantReturn.isPresent()) {
                return constantOp(op, constantReturn.orElseThrow());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<LangConstant> constantOp(OpSpTypeNode op, LangConstant constant) {
        switch (op) {
            case STR:
                if (constant instanceof StringConstant) {
                    return Optional.of(constant);
                } else {
                    return constant.strValue().map(LangConstant::of);
                }
            case BOOL:
                if (constant instanceof BoolConstant) {
                    return Optional.of(constant);
                } else {
                    return constant.boolValue().mapValues(Builtins.TRUE, Builtins.FALSE);
                }
            case REPR:
                return constant.reprValue().map(LangConstant::of);
            case INT:
                if (constant instanceof IntConstant || constant instanceof BigintConstant) {
                    return Optional.of(constant);
                } else {
                    return Optional.empty();
                }
            default:
                return Optional.empty();
        }
    }

    private static final Map<String, OpSpTypeNode> BUILTINS_TO_OPERATORS = Map.of(
            "int", OpSpTypeNode.INT,
            "str", OpSpTypeNode.STR,
            "bool", OpSpTypeNode.BOOL,
            "repr", OpSpTypeNode.REPR,
            "reversed", OpSpTypeNode.REVERSED,
            "iter", OpSpTypeNode.ITER,
            "hash", OpSpTypeNode.HASH
    );

    @NotNull
    private List<Byte> convertBuiltin(int start, String strName) {
        assert Builtins.BUILTIN_MAP.containsKey(strName);
        List<Byte> bytes = new ArrayList<>();
        if (BUILTINS_TO_OPERATORS.containsKey(strName)) {
            var params = node.getParameters();
            if (params.length != 1) {  // No operator needs more than self
                var builtin = Builtins.BUILTIN_MAP.get(strName).getType();
                var fnInfo = builtin.operatorInfo(OpSpTypeNode.CALL, AccessLevel.PUBLIC).orElseThrow();
                var expected = fnInfo.getArgs().getNormalArgs();
                throw argError(node, builtin.name(), getArgs(params), expected);
            }
            bytes.addAll(TestConverter.bytes(start + bytes.size(), params[0].getArgument(), info, 1));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) BUILTINS_TO_OPERATORS.get(strName).ordinal()));
            bytes.addAll(Util.shortZeroBytes());
            return bytes;
        } else if (strName.equals("type")) {
            var params = node.getParameters();
            if (params.length != 1) {
                throw CompilerException.format(
                        "'type' can only be called with 1 argument, not %d", node, params.length
                );
            }
            var converter = TestConverter.of(info, params[0].getArgument(), 1);
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.GET_TYPE.value);
            return bytes;
        } else {
            throw new RuntimeException();
        }
    }

    @NotNull
    private List<Byte> convertCallFn(
            int start, String strName, FunctionInfo fnInfo, Set<Integer> needsMakeOption, boolean tail
    ) {
        var fnIndex = info.fnIndex(strName);
        assert fnIndex != -1;
        if (info.fnInfo(strName).orElseThrow().isDeprecated()) {
            CompilerWarning.warnf("Function '%s' is deprecated", WarningType.DEPRECATED, info, node, strName);
        }
        List<Byte> bytes = new ArrayList<>();
        convertArgs(bytes, start, fnInfo, needsMakeOption);
        bytes.add(tail ? Bytecode.TAIL_FN.value : Bytecode.CALL_FN.value);
        bytes.addAll(Util.shortToBytes(fnIndex));
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        return bytes;
    }

    public static Pair<List<Byte>, Integer> convertArgs(
            CompilerInfo info, Lined node, int start, TypeObject caller, ArgumentNode[] args
    ) {
        var pair = ensureTypesMatch(info, node, caller, args);
        var fnInfo = pair.getKey();
        var swaps = pair.getValue();
        var argPositions = fnInfo.getArgs().argPositions(getArgs(info, args));
        List<Byte> bytes = new ArrayList<>();
        var argc = convertInnerArgs(info, bytes, start, args, argPositions, swaps);
        return Pair.of(bytes, argc);
    }

    /**
     * Calculates the sequence of swaps in order to make {@code values} be in
     * strictly ascending order.
     * <p>
     *     This assumes that {@code values} contains all ints from {@code 0} to
     *     {@code values.length - 1}; i.e. {@code list(sorted(values)) ==
     *     list([0:values.length])}.
     * </p>
     *
     * @param values The values to figure out how to sort
     * @return The pairs of values to swap
     */
    @NotNull
    private static List<Pair<Integer, Integer>> swapsToOrder(int... values) {
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

    private static void arrSwap(int[] values, int a, int b) {
        int temp = values[a];
        values[a] = values[b];
        values[b] = temp;
    }
}
