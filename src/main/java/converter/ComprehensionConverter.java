package main.java.converter;

import main.java.parser.ComprehensionNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ComprehensionConverter implements TestConverter {  // TODO: Generators
    private final ComprehensionNode node;
    private final CompilerInfo info;
    private final int retCount;

    public ComprehensionConverter(CompilerInfo info, ComprehensionNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    private enum BraceType {
        LIST("list", Builtins.LIST, Bytecode.LIST_CREATE, Bytecode.LIST_ADD, true),
        SET("set", Builtins.SET, Bytecode.SET_CREATE, Bytecode.SET_ADD, true),
        GENERATOR("generator", Builtins.ITERABLE, null, Bytecode.YIELD, false),
        ;

        final String name;
        final TypeObject type;
        final Bytecode createCode;
        final Bytecode addCode;
        final boolean addSwap;

        BraceType(String name, TypeObject type, Bytecode createCode, Bytecode addCode, boolean addSwap) {
            this.name = name;
            this.type = type;
            this.createCode = createCode;
            this.addCode = addCode;
            this.addSwap = addSwap;
        }

        static BraceType fromBrace(@NotNull String brace, Lined lineInfo) {
            switch (brace) {
                case "[":
                    return BraceType.LIST;
                case "{":
                    return BraceType.SET;
                case "(":
                    return BraceType.GENERATOR;
                default:
                    throw CompilerInternalError.format("Unknown brace type %s", lineInfo, brace);
            }
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var resultType = BraceType.fromBrace(node.getBrace(), node).type;
        var variable = node.getVariables()[0];
        if (variable instanceof TypedVariableNode) {
            var typedVariable = (TypedVariableNode) variable;
            info.addStackFrame();
            var name = typedVariable.getVariable().getName();
            if (Builtins.FORBIDDEN_NAMES.contains(name)) {
                throw CompilerException.format(
                        "Illegal name for variable '%s'", typedVariable.getVariable(), name
                );
            }
            info.checkDefinition(name, variable);
            var trueType = varType(typedVariable);
            info.addVariable(name, trueType, variable);
            var result = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1);
            info.removeStackFrame();
            return new TypeObject[] {resultType.generify(result).makeMut()};
        } else {
            var retType = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1);
            return new TypeObject[] {resultType.generify(retType).makeMut()};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        var braceType = BraceType.fromBrace(node.getBrace(), node);
        if (braceType == BraceType.GENERATOR) {
            var bytes = innerConvert(0, braceType);
            bytes.add(Bytecode.RETURN.value);
            bytes.addAll(Util.shortZeroBytes());
            var fnInfo = new FunctionInfo(info.generatorName(), returnType());
            var fnNo = info.addFunction(new Function(fnInfo, bytes, true));
            List<Byte> trueBytes = new ArrayList<>();
            trueBytes.add(Bytecode.MAKE_FUNCTION.value);
            trueBytes.addAll(Util.shortToBytes((short) fnNo));
            trueBytes.add(Bytecode.CALL_TOS.value);
            trueBytes.addAll(Util.shortZeroBytes());
            return trueBytes;
        } else {
            return innerConvert(start, braceType);
        }
    }

    @NotNull
    private List<Byte> innerConvert(int start, @NotNull BraceType braceType) {
        assert retCount == 1 || retCount == 0;
        List<Byte> bytes = new ArrayList<>();
        if (braceType.createCode != null) {
            bytes.add(braceType.createCode.value);
            bytes.addAll(Util.shortToBytes((short) 0));
        }
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
            info.checkDefinition(typedVar.getVariable().getName(), variable);
            var trueType = varType(typedVar);
            info.addVariable(typedVar.getVariable().getName(), trueType, variable);
        }
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(variable.getVariable().getName())));
        if (!node.getCondition().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getCondition(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            bytes.addAll(Util.intToBytes(topJump));
        }
        int whileJmp = addWhileCond(start, bytes);
        if (braceType.addSwap) {
            bytes.add(Bytecode.SWAP_2.value);  // The iterator object will be atop the list, swap it and back again
        }
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getBuilder()[0].getArgument(), info, 1));
        bytes.add(braceType.addCode.value);
        if (braceType.addCode == Bytecode.YIELD) {
            bytes.addAll(Util.shortToBytes((short) 1));
        }
        if (braceType.addSwap) {
            bytes.add(Bytecode.SWAP_2.value);
        }
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(topJump));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), forJump);
        if (whileJmp != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), whileJmp);
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        info.removeStackFrame();
        return bytes;
    }

    private int addWhileCond(int start, List<Byte> bytes) {
        if (!node.getWhileCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getWhileCond(), info, 1));
            bytes.add(Bytecode.JUMP_TRUE.value);
            int innerWhileJump = bytes.size();
            bytes.addAll(Util.zeroToBytes());
            bytes.add(Bytecode.POP_TOP.value);
            bytes.add(Bytecode.JUMP.value);
            int whileJmp = bytes.size();
            bytes.addAll(Util.zeroToBytes());
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), innerWhileJump);
            return whileJmp;
        } else {
            return -1;
        }
    }

    private TypeObject varType(@NotNull TypedVariableNode typedVar) {
        var tvType = typedVar.getType();
        return tvType.isDecided()
                ? info.getType(tvType)
                : TestConverter.returnType(node.getLooped().get(0), info, 1)[0]
                    .operatorReturnType(OpSpTypeNode.ITER, info)[0];
    }
}
