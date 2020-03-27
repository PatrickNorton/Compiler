package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FunctionCallConverter implements TestConverter {
    private CompilerInfo info;
    private FunctionCallNode node;
    private int retCount;

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
            return info.getType(name).operatorReturnType(OpSpTypeNode.CALL);
        } else {
            return TestConverter.returnType(node.getCaller(), info, retCount)[0].operatorReturnType(OpSpTypeNode.CALL);
        }

    }

    private void ensureTypesMatch(@NotNull TypeObject callerType) {
        var params = node.getParameters();
        var args = new Argument[node.getParameters().length];
        for (int i = 0; i < args.length; i++) {
            var type = TestConverter.returnType(params[i].getArgument(), info, 1)[0];
            args[i] = new Argument(params[i].getVariable().getName(), type);
        }
        var operatorInfo = callerType.operatorInfo(OpSpTypeNode.CALL);
        if (operatorInfo == null || !operatorInfo.matches(args)) {
            throw CompilerException.format(
                    "Cannot call '%s', arguments given do not match the arguments of the function",
                    node, node.getCaller()
            );
        }
    }
}
