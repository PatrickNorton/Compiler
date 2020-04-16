package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
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
        ensureTypesMatch(retTypes[0]);
        if (isDeterminedFunction(node.getCaller())) {
            return convertOptimized(start);
        }
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        convertCall(bytes, start);
        for (int i = retCount; i < returnType().length; i++) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    void convertCall(List<Byte> bytes, int start) {
        for (var value : node.getParameters()) {
            // TODO: Varargs
            bytes.addAll(TestConverter.bytes(start + bytes.size(), value.getArgument(), info, 1));
        }
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
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
                return new TypeObject[]{cls};
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

    private void ensureTypesMatch(@NotNull TypeObject callerType) {
        var params = node.getParameters();
        var args = new Argument[node.getParameters().length];
        for (int i = 0; i < args.length; i++) {
            var type = TestConverter.returnType(params[i].getArgument(), info, 1)[0];
            args[i] = new Argument(params[i].getVariable().getName(), type);
        }
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
    private List<Byte> convertOptimized(int start) {
        assert node.getCaller() instanceof VariableNode;
        var strName = ((VariableNode) node.getCaller()).getName();
        if (Builtins.BUILTIN_MAP.containsKey(strName)) {
            return convertBuiltin(start, strName);
        } else {
            return convertCallFn(start, strName);
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
    private List<Byte> convertCallFn(int start, String strName) {
        var fnIndex = info.fnIndex(strName);
        assert fnIndex != -1;
        List<Byte> bytes = new ArrayList<>();
        for (var value : node.getParameters()) {
            // TODO: Varargs
            bytes.addAll(TestConverter.bytes(start + bytes.size(), value.getArgument(), info, 1));
        }
        bytes.add(Bytecode.CALL_FN.value);
        bytes.addAll(Util.shortToBytes(fnIndex));
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        return bytes;
    }
}
