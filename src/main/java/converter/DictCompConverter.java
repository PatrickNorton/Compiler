package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.parser.DictComprehensionNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;

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
            return new TypeObject[] {Builtins.dict().generify(keyType, valType).makeMut()};
        } else {
            var keyType = TestConverter.returnType(node.getKey(), info, 1)[0];
            var valType = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1)[0];
            return new TypeObject[] {Builtins.dict().generify(keyType, valType).makeMut()};
        }
    }

    @NotNull
    @Override
    public BytecodeList convert() {  // TODO: Refactor with ComprehensionConverter (and ForConverter?)
        if (retCount == 0) {
            CompilerWarning.warn("Unnecessary dict creation", WarningType.UNUSED, info, node);
        } else if (retCount > 1) {
            throw CompilerException.format("Dict comprehension only returns one value, got %d", node, retCount);
        }
        BytecodeList bytes = new BytecodeList();
        bytes.add(Bytecode.DICT_CREATE, 0);
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.iterConstant()));
        bytes.addAll(TestConverter.bytes(node.getLooped().get(0), info, 1));
        bytes.add(Bytecode.CALL_TOS, 1);
        var topJump = info.newJumpLabel();
        bytes.addLabel(topJump);
        var forJump = info.newJumpLabel();
        bytes.add(Bytecode.FOR_ITER, forJump, new ArgcBytecode((short) 1));
        // Add the variable for the loop
        var variable = node.getVariables()[0];
        info.addStackFrame();
        if (variable instanceof TypedVariableNode) {
            var typedVar = (TypedVariableNode) variable;
            info.checkDefinition(typedVar.getVariable().getName(), typedVar);
            info.addVariable(typedVar.getVariable().getName(), info.getType(typedVar.getType()), typedVar);
        }
        bytes.add(Bytecode.STORE, info.varIndex(variable.getVariable()));
        if (!node.getCondition().isEmpty()) {
            bytes.addAll(TestConverter.bytes(node.getCondition(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE, topJump);
        }
        bytes.add(Bytecode.SWAP_2);  // The iterator object will be atop the list, swap it and back again
        bytes.addAll(TestConverter.bytes(node.getKey(), info, 1));
        bytes.addAll(TestConverter.bytes(node.getBuilder()[0].getArgument(), info, 1));
        bytes.add(Bytecode.DICT_ADD);
        bytes.add(Bytecode.SWAP_2);
        bytes.add(Bytecode.JUMP, topJump);
        bytes.addLabel(forJump);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        info.removeStackFrame();
        return bytes;
    }
}
