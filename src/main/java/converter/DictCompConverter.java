package main.java.converter;

import main.java.parser.DictComprehensionNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DictCompConverter implements TestConverter {
    private final DictComprehensionNode node;
    private final CompilerInfo info;
    private final int retCount;

    public DictCompConverter(CompilerInfo info, DictComprehensionNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var variable = node.getVariables()[0];
        if (variable instanceof TypedVariableNode) {
            var typedVariable = (TypedVariableNode) variable;
            info.addStackFrame();
            info.addVariable(typedVariable.getVariable().getName(), info.getType(typedVariable.getType()), typedVariable);
            var keyType = TestConverter.returnType(node.getKey(), info, 1)[0];
            var valType = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1)[0];
            info.removeStackFrame();
            return new TypeObject[] {Builtins.DICT.generify(keyType, valType).makeMut()};
        } else {
            var keyType = TestConverter.returnType(node.getKey(), info, 1)[0];
            var valType = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1)[0];
            return new TypeObject[] {Builtins.DICT.generify(keyType, valType).makeMut()};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {  // TODO: Refactor with ComprehensionConverter (and ForConverter?)
        assert retCount == 1 || retCount == 0;
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.DICT_CREATE.value);
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
        bytes.addAll(Util.shortToBytes((short) 1));
        // Add the variable for the loop
        var variable = node.getVariables()[0];
        info.addStackFrame();
        if (variable instanceof TypedVariableNode) {
            var typedVar = (TypedVariableNode) variable;
            info.checkDefinition(typedVar.getVariable().getName(), typedVar);
            info.addVariable(typedVar.getVariable().getName(), info.getType(typedVar.getType()), typedVar);
        }
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(variable.getVariable().getName())));
        if (!node.getCondition().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getCondition(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            bytes.addAll(Util.intToBytes(topJump));
        }
        bytes.add(Bytecode.SWAP_2.value);  // The iterator object will be atop the list, swap it and back again
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getKey(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getBuilder()[0].getArgument(), info, 1));
        bytes.add(Bytecode.DICT_ADD.value);
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
