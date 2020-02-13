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
        assert retCount == 1 || retCount == 0;  // TODO: Multiple returns
        var callConverter = TestConverter.of(info, node.getCaller(), 1);
        // ensureTypesMatch(callConverter.returnType());
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        convertCall(bytes, start);
        return bytes;
    }

    void convertCall(List<Byte> bytes, int start) {
        for (var value : node.getParameters()) {
            // TODO: Varargs
            bytes.addAll(TestConverter.bytes(start + bytes.size(), value.getArgument(), info, 1));
        }
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        // TODO: Pop when return type is not void
//        if (retCount == 0) {
//            bytes.add(Bytecode.POP_TOP.value);
//        }
    }

    @Override
    public TypeObject returnType() {
        if (node.getCaller() instanceof VariableNode) {
            var name = node.getVariable().getName();
            if (info.varIsUndefined(name)) {
                throw CompilerException.format("Undefined variable '%s'", node, name);
            }
            var cls = info.classOf(name);
            if (cls != null) {  // If the variable is a class, calling it will always return an instance
                return cls;
            }
            var fn = info.fnInfo(name);
            if (fn != null) {
                return fn.getReturns()[0];
            }
            return info.getType(name).operatorReturnType(OpSpTypeNode.CALL);
        } else {
            return TestConverter.returnType(node.getCaller(), info, retCount).operatorReturnType(OpSpTypeNode.CALL);
        }

    }

    private void ensureTypesMatch(@NotNull TypeObject callerType) {
        if (!callerType.isSubclass(Builtins.CALLABLE)) {
            throw CompilerException.of("Cannot call " + node.getCaller(), node);
        }
    }
}
