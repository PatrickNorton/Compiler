package main.java.converter;

import main.java.parser.ComprehensionNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ComprehensionConverter implements TestConverter {
    private final ComprehensionNode node;
    private final CompilerInfo info;
    private final int retCount;

    public ComprehensionConverter(CompilerInfo info, ComprehensionNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    private enum BraceType {
        LIST("list", Bytecode.LIST_CREATE, Bytecode.LIST_ADD, true),
        SET("set", Bytecode.SET_CREATE, Bytecode.SET_ADD, true),
        GENERATOR("generator", null, Bytecode.YIELD, false),
        ;

        final String name;
        final Bytecode createCode;
        final Bytecode addCode;
        final boolean addSwap;

        BraceType(String name, Bytecode createCode, Bytecode addCode, boolean addSwap) {
            this.name = name;
            this.createCode = createCode;
            this.addCode = addCode;
            this.addSwap = addSwap;
        }

        TypeObject type() {
            switch (this) {
                case LIST:
                    return Builtins.list();
                case SET:
                    return Builtins.set();
                case GENERATOR:
                    return Builtins.iterable();
                default:
                    throw new UnsupportedOperationException();
            }
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
        var resultType = BraceType.fromBrace(node.getBrace(), node).type();
        return new TypeObject[] {resultType.generify(genericType()).makeMut()};
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (retCount > 1) {
            throw CompilerException.format("Comprehension only returns 1 value, expected %d", node, retCount);
        }
        var braceType = BraceType.fromBrace(node.getBrace(), node);
        if (braceType == BraceType.GENERATOR) {
            if (retCount == 0) {
                // Comprehensions that are not called have no side effects, so no need to deal with it
                CompilerWarning.warn(
                        "Comprehension with no returns serves no purpose", WarningType.UNUSED, info, node
                );
                return new BytecodeList();
            }
            var bytes = innerConvert(braceType);
            bytes.add(Bytecode.RETURN, 0);
            var fnInfo = new FunctionInfo(info.generatorName(), true, returnType());
            var fnNo = info.addFunction(new Function(node, fnInfo, bytes));
            BytecodeList trueBytes = new BytecodeList();
            trueBytes.add(Bytecode.MAKE_FUNCTION, fnNo);
            trueBytes.add(Bytecode.CALL_TOS, 0);
            return trueBytes;
        } else {
            return innerConvert(braceType);
        }
    }

    @NotNull
    private BytecodeList innerConvert(@NotNull BraceType braceType) {
        assert retCount == 1 || retCount == 0;
        var bytes = new BytecodeList();
        if (braceType.createCode != null) {
            bytes.addAll(new TypeLoader(node.getLineInfo(), genericType(), info).convert());
            bytes.add(braceType.createCode, 0);
        }
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.iterConstant()));
        bytes.addAll(TestConverter.bytes(node.getLooped().get(0), info, 1));
        bytes.add(Bytecode.CALL_TOS, 1);
        var topJump = info.newJumpLabel();
        bytes.addLabel(topJump);
        var forJump = info.newJumpLabel();
        bytes.add(Bytecode.FOR_ITER, forJump, 1);
        if (node.getVariables().length > 1) {
            throw CompilerTodoError.of("Cannot convert comprehension with more than one variable yet", node);
        }
        // Add the variable for the loop
        var variable = node.getVariables()[0];
        info.addStackFrame();
        if (variable instanceof TypedVariableNode) {
            var typedVar = (TypedVariableNode) variable;
            info.checkDefinition(typedVar.getVariable().getName(), variable);
            var trueType = varType(typedVar);
            info.addVariable(typedVar.getVariable().getName(), trueType, variable);
        }
        bytes.add(Bytecode.STORE, info.varIndex(variable.getVariable()));
        if (!node.getCondition().isEmpty()) {
            bytes.addAll(TestConverter.bytes(node.getCondition(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE, topJump);
        }
        var whileJmp = addWhileCond(bytes);
        if (braceType.addSwap) {
            bytes.add(Bytecode.SWAP_2);  // The iterator object will be atop the list, swap it and back again
        }
        bytes.addAll(TestConverter.bytes(node.getBuilder()[0].getArgument(), info, 1));
        if (braceType.addCode == Bytecode.YIELD) {
            bytes.add(Bytecode.YIELD, 1);
        } else {
            bytes.add(braceType.addCode);
        }
        if (braceType.addSwap) {
            bytes.add(Bytecode.SWAP_2);
        }
        bytes.add(Bytecode.JUMP, topJump);
        bytes.addLabel(forJump);
        if (whileJmp != null) {
            bytes.addLabel(whileJmp);
        }
        if (retCount == 0) {
            assert braceType != BraceType.GENERATOR;
            bytes.add(Bytecode.POP_TOP);
        }
        info.removeStackFrame();
        return bytes;
    }

    @Nullable
    private Label addWhileCond(BytecodeList bytes) {
        if (!node.getWhileCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(node.getWhileCond(), info, 1));
            var innerWhileJump = info.newJumpLabel();
            bytes.add(Bytecode.JUMP_TRUE, innerWhileJump);
            bytes.add(Bytecode.POP_TOP);
            var whileJump = info.newJumpLabel();
            bytes.add(Bytecode.JUMP, whileJump);
            bytes.addLabel(innerWhileJump);
            return whileJump;
        } else {
            return null;
        }
    }

    private TypeObject varType(@NotNull TypedVariableNode typedVar) {
        var tvType = typedVar.getType();
        if (tvType.isDecided()) {
            return info.getType(tvType);
        } else {
            var retType = TestConverter.returnType(node.getLooped().get(0), info, 1)[0];
            var opRet = retType.tryOperatorReturnType(node.getLooped().get(0), OpSpTypeNode.ITER, info)[0];
            return Builtins.deIterable(opRet)[0];
        }
    }

    private TypeObject genericType() {
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
            return result[0];
        } else {
            var retType = TestConverter.returnType(node.getBuilder()[0].getArgument(), info, 1);
            return retType[0];
        }
    }
}
