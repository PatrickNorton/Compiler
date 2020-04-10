package main.java.converter;

import main.java.parser.ComprehensionNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ComprehensionConverter implements TestConverter {
    private final ComprehensionNode node;
    private final CompilerInfo info;
    private final int retCount;

    public ComprehensionConverter(CompilerInfo info, ComprehensionNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var resultType = node.getBrace().equals("[") ? Builtins.LIST : Builtins.SET;
        var variable = node.getVariables()[0];
        if (variable instanceof TypedVariableNode) {
            var typedVariable = (TypedVariableNode) variable;
            info.addStackFrame();
            var name = typedVariable.getVariable().getName();
            if (Builtins.FORBIDDEN_NAMES.contains(name)) {
                throw CompilerException.format("Illegal name for variable '%s'", typedVariable.getVariable(), name);
            }
            info.addVariable(name, info.getType(typedVariable.getType()));
            var result = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1);
            info.removeStackFrame();
            return new TypeObject[] {resultType.generify(result)};
        } else {
            return new TypeObject[] {resultType.generify(TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1))};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {  // TODO: While conditional
        assert retCount == 1 || retCount == 0;
        boolean isList = node.getBrace().equals("[");
        List<Byte> bytes = new ArrayList<>();
        bytes.add(isList ? Bytecode.LIST_CREATE.value : Bytecode.SET_CREATE.value);
        bytes.addAll(Util.shortToBytes((short) 0));
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("iter"))));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getLooped().get(0), info, 1));
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        var topJump = start + bytes.size();
        bytes.add(Bytecode.FOR_ITER.value);
        int forJump = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        // Add the variable for the loop
        var variable = node.getVariables()[0];
        info.addStackFrame();
        if (variable instanceof TypedVariableNode) {
            var typedVar = (TypedVariableNode) variable;
            info.addVariable(typedVar.getVariable().getName(), info.getType(typedVar.getType()));
        }
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(variable.getVariable().getName())));
        if (!node.getCondition().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getCondition(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            bytes.addAll(Util.intToBytes(topJump));
        }
        bytes.add(Bytecode.SWAP_2.value);  // The iterator object will be atop the list, swap it and back again
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getBuilder()[0].getArgument(), info, 1));
        bytes.add(isList ? Bytecode.LIST_ADD.value : Bytecode.SET_ADD.value);
        bytes.add(Bytecode.SWAP_2.value);
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(topJump));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), forJump);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        info.removeStackFrame();
        return bytes;
    }
}
