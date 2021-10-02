package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.ForStatementNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TypedVariableNode;
import main.java.parser.VarLikeNode;
import main.java.parser.VariableNode;
import main.java.util.Levenshtein;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ForConverter extends LoopConverter {
    private final ForStatementNode node;
    private final CompilerInfo info;

    public ForConverter(CompilerInfo info, ForStatementNode node) {
        super(info);
        this.node = node;
        this.info = info;
    }

    @Override
    protected BytecodeList trueConvert() {
        return trueConvertWithReturn().getKey();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Pair<BytecodeList, DivergingInfo> trueConvertWithReturn() {
        var varLen = node.getVars().length;
        var iterLen = node.getIterables().size();
        if (iterLen > varLen) {
            throw CompilerException.format(
                    "For-loops may not have more iterables than variables%n"
                            + "(got %d iterables, %d variables)",
                    node, iterLen, varLen
            );
        } else if (iterLen == 1) {
            return convertSingleIter();
        } else {
            return convertMultipleIter();
        }
    }

    @NotNull
    private Pair<BytecodeList, DivergingInfo> convertSingleIter() {
        var retCount = node.getVars().length;
        var valueConverter = TestConverter.of(info, node.getIterables().get(0), 1);
        var bytes = new BytecodeList();
        addIter(info, bytes, valueConverter);
        bytes.addLabel(info.loopManager().continueLabel());
        var topLabel = info.newJumpLabel();
        bytes.addLabel(topLabel);
        var label = info.newJumpLabel();
        bytes.add(Bytecode.FOR_ITER, label, new ArgcBytecode((short) retCount));
        for (int i = retCount - 1; i >= 0; i--) {
            addVariableStorage(bytes, i, valueConverter, false);
        }
        var divergingInfo = addCleanup(label, bytes);
        return Pair.of(bytes, divergingInfo);
    }

    @NotNull
    private Pair<BytecodeList, DivergingInfo> convertMultipleIter() {
        var varLen = node.getVars().length;
        var iterLen = node.getIterables().size();
        if (varLen != iterLen) {
            throw CompilerException.of(
                    """
                    For loops with more than one iterable must have an equal number of variables and iterables

                    Note: Statements with multiple returns are only usable in for-loops when there is only one""",
                    node
            );
        }
        List<TestConverter> valueConverters = new ArrayList<>();
        var bytes = new BytecodeList();
        for (int i = 0; i < varLen; i++) {
            var iterator = node.getIterables().get(i);
            var valueConverter = TestConverter.of(info, iterator, 1);
            valueConverters.add(valueConverter);
            addIter(info, bytes, valueConverter);
        }
        bytes.addLabel(info.loopManager().continueLabel());
        var label = info.newJumpLabel();
        bytes.add(Bytecode.FOR_PARALLEL, label, new ArgcBytecode((short) varLen));
        assert valueConverters.size() == varLen;
        for (int i = varLen - 1; i >= 0; i--) {
            var valueConverter = valueConverters.get(i);
            addVariableStorage(bytes, i, valueConverter, true);
        }
        var divergingInfo = addCleanup(label, bytes);
        return Pair.of(bytes, divergingInfo);
    }

    private void addVariableStorage(BytecodeList bytes, int i, TestConverter valueConverter, boolean firstRet) {
        var typedVar = node.getVars()[i];
        var iteratorType = getIteratorType(i, valueConverter, firstRet);
        var variable = typedVar.getVariable();
        var iteratedName = variable.toString();
        if (typedVar instanceof TypedVariableNode) {
            if (Builtins.FORBIDDEN_NAMES.contains(iteratedName)) {
                throw CompilerException.format("Illegal name for variable '%s'", variable, iteratedName);
            }
            var valueType = returnType(firstRet ? 0 : i, valueConverter);
            if (!iteratorType.isSuperclass(valueType)) {
                throw CompilerException.format(
                        "Object of type '%s' cannot be assigned to object of type '%s'",
                        node, valueType.name(), iteratorType.name());
            }
            info.checkDefinition(iteratedName, typedVar);
            info.addVariable(iteratedName, iteratorType, typedVar.getVariable());
        } else {
            assert typedVar instanceof VariableNode;
            var name = ((VariableNode) typedVar).getName();
            if (info.variableIsImmutable(name)) {
                throw CompilerException.format("Cannot assign to immutable variable '%s'", node, name);
            }
        }
        bytes.add(Bytecode.STORE, new VariableBytecode(info.varIndex(variable)));
    }

    @NotNull
    private DivergingInfo addCleanup(Label label, @NotNull BytecodeList bytes) {
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
        var divergingInfo = pair.getValue();
        bytes.addAll(pair.getKey());
        bytes.add(Bytecode.JUMP, info.loopManager().continueLabel());
        bytes.addLabel(label);
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes no more than once", WarningType.UNREACHABLE, info, node);
        }
        if (node.getNobreak().isEmpty()) {
            divergingInfo.makeUncertain();
        } else {
            var nobreak = BaseConverter.bytesWithReturn(node.getNobreak(), info);
            bytes.addAll(nobreak.getKey());
            divergingInfo.andWith(nobreak.getValue());
        }
        return divergingInfo;
    }

    private TypeObject returnType(int i, @NotNull TestConverter valueConverter) {
        var opTypes = valueConverter.returnType()[0].tryOperatorReturnType(node, OpSpTypeNode.ITER, info);
        var opType = Builtins.deIterable(opTypes[0]);
        if (opType.length <= i) {
            throw CompilerException.format(
                    "Expected at least %d returns from iterable, got %d",
                    node, opType.length, i + 1
            );
        }
        return opType[i];
    }

    private TypeObject getIteratorType(int i, TestConverter valueConverter, boolean firstRet) {
        var variable = node.getVars()[i];
        if (variable instanceof VariableNode) {
            return info.getType(variable.getVariable().getName()).orElseThrow(
                    () -> variableException(variable, info.definedNames())
            );
        }
        var iteratorType = node.getVars()[i].getType();
        if (iteratorType.isDecided()) {
            return info.getType(iteratorType);
        } else {
            return returnType(firstRet ? 0 : i, valueConverter);
        }
    }

    static void addIter(@NotNull CompilerInfo info, @NotNull BytecodeList bytes, @NotNull TestConverter converter) {
        bytes.loadConstant(Builtins.iterConstant(), info);
        bytes.addAll(convertIter(converter));
        bytes.add(Bytecode.CALL_TOS, ArgcBytecode.one());
    }

    private static BytecodeList convertIter(TestConverter converter) {
        if (converter instanceof IndexConverter && ((IndexConverter) converter).isSlice()) {
            return ((IndexConverter) converter).convertIterSlice();
        } else {
            return converter.convert();
        }
    }

    private static CompilerException variableException(VarLikeNode variable, Iterable<String> names) {
        var name = variable.getVariable().getName();
        var closest = Levenshtein.closestName(name, names);
        if (closest.isPresent()) {
            return CompilerException.format(
                    "Variable %s not defined. Did you mean %s?%n" +
                            "Help: If not, consider adding 'var' before the variable",
                    variable, name, closest.orElseThrow()
            );
        } else {
            return  CompilerException.format(
                    "Variable %s not defined%n" +
                            "Help: consider adding 'var' before the variable",
                    variable, name
            );
        }
    }
}
