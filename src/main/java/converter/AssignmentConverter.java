package main.java.converter;

import main.java.parser.AssignableNode;
import main.java.parser.AssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IndexNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Levenshtein;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AssignmentConverter implements BaseConverter {
    private final CompilerInfo info;
    private final AssignmentNode node;

    public AssignmentConverter(CompilerInfo info, AssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (node.isColon()) {
            throw CompilerException.of("Colon assignment is not supported outside of class definitions", node);
        }
        if (node.getNames().length > 1 && node.getValues().size() == 1) {
            return assignSingleVariable();
        } else {
            return assignMultipleVariable();
        }
    }

    @NotNull
    private BytecodeList assignMultipleVariable() {
        var names = node.getNames();
        var values = node.getValues();
        if (names.length != values.size()) {
            throw CompilerException.format(
                    "Multiple returns are not supported in = statements " +
                            "with more than one operand (expected %d, got %d)",
                    node, names.length, values.size()
            );
        }
        var assignBytes = new BytecodeList();
        var storeBytes = new BytecodeList(names.length * Bytecode.STORE.size());
        for (int i = 0; i < names.length; i++) {
            var name = names[i];
            var value = values.get(i);
            var valueConverter = TestConverter.of(info, value, 1);
            if (name instanceof VariableNode) {
                var varName = ((VariableNode) name).getName();
                var varType = info.getType(varName).orElseThrow(() -> defError(varName, (VariableNode) name));
                var valConverter = TestConverter.of(info, value, 1, varType);
                assignToVariable(assignBytes, storeBytes, (VariableNode) name, valConverter);
            } else if (name instanceof IndexNode) {
                assignToIndex(assignBytes, storeBytes, (IndexNode) name, valueConverter);
            } else if (name instanceof DottedVariableNode) {
                var last = ((DottedVariableNode) name).getLast();
                if (last.getPostDot() instanceof IndexNode) {
                    assignToDotIndex(assignBytes, storeBytes, (DottedVariableNode) name, valueConverter);
                } else if (last.getPostDot() instanceof VariableNode) {
                    assignToDot(assignBytes, storeBytes, (DottedVariableNode) name, value);
                } else if (last.getPostDot() instanceof SpecialOpNameNode) {
                    throw CompilerException.of("Cannot assign to constant value", node);
                } else {
                    var clsName = last.getPostDot().getClass().getName();
                    throw CompilerInternalError.format("Illegal node %s", node, clsName);
                }
            } else {
                throw CompilerException.of("Assignment must be to a variable, index, or dotted variable", node);
            }
        }
        assignBytes.addAll(storeBytes);
        return assignBytes;
    }

    @NotNull
    private BytecodeList assignSingleVariable() {
        assert node.getValues().size() == 1;
        var names = node.getNames();
        var value = node.getValues().get(0);
        var valueConverter = TestConverter.of(info, value, node.getNames().length);
        var retTypes = valueConverter.returnType();
        var bytes = new BytecodeList(valueConverter.convert());
        var nonVariableCount = quickNonVarCount(names);
        assert nonVariableCount >= 0;
        // For this section, we need to take care of possible side-effects and
        // the order in which they occur.
        // Assignment to indices (via operator []=) and fields (via properties)
        // may contain side-effects, and the language specifies that such
        // assignments should occur in the order in which they were specified
        // in the code.
        // Complicating our lives, however, is the fact that with multiple
        // returns, the stack is in reverse order from what we want. Since in
        // the majority of cases there will be no side-effects, we want to
        // optimize away the stack reversal whenever possible.
        if (nonVariableCount == 0) {
            // All variables, guaranteed side-effect free, so optimize reverse
            // order
            for (int i = names.length - 1; i >= 0; i--) {
                var name = (VariableNode) names[i];
                assignTopToVariable(bytes, name, retTypes[i]);
            }
        } else if (nonVariableCount == 1) {
            // Only 1 possible side-effect, cannot be switched with another
            for (int i = names.length - 1; i >= 0; i--) {
                assignTop(bytes, retTypes[i], names[i]);
            }
        } else {
            // Have to swap everything (sigh)
            for (int i = 0; i < names.length; i++) {
                bringToTop(bytes, names.length - i - 1);
                assignTop(bytes, retTypes[i], names[i]);
            }
        }
        return bytes;
    }

    private void assignTop(BytecodeList bytes, TypeObject retType, AssignableNode name) {
        if (name instanceof VariableNode) {
            assignTopToVariable(bytes, (VariableNode) name, retType);
        } else if (name instanceof IndexNode) {
            assignTopToIndex(bytes, (IndexNode) name, retType);
        } else if (name instanceof DottedVariableNode) {
            assignTopToDot(bytes, (DottedVariableNode) name, retType);
        } else {
            throw CompilerException.of("Assignment must be to a variable, index, or dotted variable", node);
        }
    }

    private void assignTopToVariable(BytecodeList bytes, @NotNull VariableNode variable, TypeObject valueType) {
        var name = variable.getName();
        checkDef(name, variable);
        var varType = info.getType(name).orElseThrow();
        if (!varType.isSuperclass(valueType)) {
            if (!OptionTypeObject.needsMakeOption(varType, valueType)) {
                throw CompilerException.format("Cannot assign value of type '%s' to variable of type '%s'",
                        node, valueType.name(), varType.name());
            } else {
                bytes.add(Bytecode.MAKE_OPTION);
            }
        }
        bytes.addFirst(Bytecode.STORE, info.varIndex(variable));
    }

    private void assignToVariable(@NotNull BytecodeList bytes, BytecodeList storeBytes,
                                  @NotNull VariableNode variable, @NotNull TestConverter valueConverter) {
        var valT = valueConverter.returnType();
        var valueType = valT[0];
        var name = variable.getName();
        checkDef(name, variable);
        var varType = info.getType(name).orElseThrow();
        if (!varType.isSuperclass(valueType)) {
            if (!OptionTypeObject.needsMakeOption(varType, valueType)) {
                if (varType.makeMut().isSuperclass(valueType)) {
                    throw CompilerException.format("Cannot assign: Value must be 'mut' or 'final'", node);
                }
                throw CompilerException.format("Cannot assign value of type '%s' to variable of type '%s'",
                        node, valueType.name(), varType.name());
            } else {
                bytes.addAll(OptionTypeObject.wrapBytes(valueConverter.convert()));
            }
        } else {
            bytes.addAll(valueConverter.convert());
        }
        storeBytes.addFirst(Bytecode.STORE, info.varIndex(variable));
    }

    private void checkDef(String name, VariableNode variable) {
        if (info.varIsUndefined(name)) {
            throw defError(name, variable);
        }
        if (info.variableIsImmutable(name)) {
            throw CompilerException.format("Cannot assign to const variable %s", variable, name);
        }
    }

    private CompilerException defError(String name, VariableNode variable) {
        var closest = Levenshtein.closestName(name, info.definedNames());
        if (closest.isPresent()) {
            return CompilerException.format(
                    "Attempted to assign to undefined name %s%n" +
                    "Help: Did you mean %s?", variable, name, closest.orElseThrow()
            );
        } else {
            return CompilerException.format("Attempted to assign to undefined name %s", variable, name);
        }
    }

    private void assignTopToIndex(
            @NotNull BytecodeList bytes, @NotNull IndexNode variable, TypeObject valueType
    ) {
        var indices = variable.getIndices();
        var varConverter = TestConverter.of(info, variable.getVar(), 1);
        topToIndex(bytes, varConverter, indices, valueType);
    }

    private void topToIndex(
            BytecodeList bytes, TestConverter preDot, TestNode[] indices, TypeObject valueType
    ) {
        if (IndexConverter.isSlice(indices)) {
            var index = (SliceNode) indices[0];
            checkSlice(valueType, preDot.returnType()[0]);
            bytes.addAll(preDot.convert());
            bytes.addAll(new SliceConverter(info, index).convert());
            bytes.add(Bytecode.SWAP_3);
            bytes.add(Bytecode.CALL_OP, OpSpTypeNode.SET_SLICE.ordinal(), 2);
        } else {
            var indexConverters = convertIndices(indices);
            checkTypes(preDot.returnType()[0], indexConverters, valueType);
            bytes.addAll(preDot.convert());
            for (var indexParam : indexConverters) {
                bytes.addAll(indexParam.convert());
            }
            bringToTop(bytes, indices.length + 1);
            bytes.addFirst(Bytecode.STORE_SUBSCRIPT, indices.length);
        }
    }

    private static void bringToTop(BytecodeList bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2);
                return;
            case 2:
                bytes.add(Bytecode.SWAP_3);
                return;
            default:
                bytes.add(Bytecode.SWAP_N, distFromTop + 1);
        }
    }

    private void assignToIndex(@NotNull BytecodeList bytes, BytecodeList storeBytes,
                               @NotNull IndexNode variable, @NotNull TestConverter valueConverter) {
        var indices = variable.getIndices();
        var varConverter = TestConverter.of(info, variable.getVar(), 1);
        if (IndexConverter.isSlice(indices)) {
            checkSlice(varConverter.returnType()[0], valueConverter.returnType()[0]);
            finishSlice(bytes, storeBytes, valueConverter, (SliceNode) indices[0]);
        } else {
            var indexConverters = convertIndices(indices);
            checkTypes(varConverter.returnType()[0], indexConverters, valueConverter.returnType()[0]);
            bytes.addAll(TestConverter.bytes(variable.getVar(), info, 1));
            finishIndex(bytes, storeBytes, valueConverter, indices);
        }
    }

    @NotNull
    private List<TestConverter> convertIndices(@NotNull TestNode[] indices) {
        List<TestConverter> indexConverters = new ArrayList<>(indices.length);
        for (var index : indices) {
            indexConverters.add(TestConverter.of(info, index, 1));
        }
        return indexConverters;
    }

    private void checkTypes(TypeObject varType, @NotNull List<TestConverter> values, TypeObject setType) {
        List<Argument> indexTypes = new ArrayList<>(values.size());
        for (var index : values) {
            indexTypes.add(new Argument("", index.returnType()[0]));
        }
        indexTypes.add(new Argument("", setType));
        var opInfo = varType.tryOperatorInfo(node, OpSpTypeNode.SET_ATTR, info);
        if (opInfo.generifyArgs(indexTypes.toArray(new Argument[0])).isEmpty()) {
            var nameArr = TypeObject.name(Argument.typesOf(indexTypes.toArray(new Argument[0])));
            throw indexErr(false, varType, nameArr, opInfo);
        }
    }

    private void checkSlice(TypeObject varType, TypeObject setType) {
        var ops = varType.tryOperatorInfo(node, OpSpTypeNode.SET_SLICE, info);
        var args = new Argument[] {new Argument("", Builtins.slice()), new Argument("", setType)};
        if (ops.generifyArgs(args).isEmpty()) {
            var nameArr = TypeObject.name(Builtins.slice());
            throw indexErr(true, varType, nameArr, ops);
        }
    }

    private CompilerException indexErr(boolean isSlice, TypeObject varType, String[] nameArr, FunctionInfo ops) {
        var argTypes = Argument.typesOf(ops.getArgs().getNormalArgs());
        var argsString = String.join(", ", TypeObject.name(argTypes));
        throw CompilerException.format(
                "Cannot assign variable to index: '%s'.operator [%s]= does not match the given types%n" +
                        "Arguments received: %s%nArguments expected: %s",
                node, varType.name(), isSlice ? ":" : "", String.join(", ", nameArr), argsString
        );
    }

    private void assignToDot(@NotNull BytecodeList bytes, @NotNull BytecodeList storeBytes,
                             @NotNull DottedVariableNode variable, @NotNull TestNode value) {
        var pair = DotConverter.exceptLast(info, variable, 1);
        var preDotConverter = pair.getKey();
        var assignedType = assignType(preDotConverter, pair.getValue(), variable);
        var valueConverter = TestConverter.of(info, value, 1, assignedType);
        var valueType = valueConverter.returnType()[0];
        var needsMakeOption = checkAssign(preDotConverter, pair.getValue(), valueType, variable);
        bytes.addAll(preDotConverter.convert());
        bytes.addAll(OptionTypeObject.maybeWrapBytes(valueConverter.convert(), needsMakeOption));
        storeBytes.addFirst(Bytecode.STORE_ATTR, info.constIndex(LangConstant.of(pair.getValue())));
    }

    private void assignToDotIndex(@NotNull BytecodeList bytes, @NotNull BytecodeList storeBytes,
                                  @NotNull DottedVariableNode variable, @NotNull TestConverter valueConverter) {
        var pair = DotConverter.exceptLastIndex(info, variable, 1);
        var varConverter = pair.getKey();
        var indices = pair.getValue();
        if (IndexConverter.isSlice(indices)) {
            checkSlice(varConverter.returnType()[0], valueConverter.returnType()[0]);
            finishSlice(bytes, storeBytes, valueConverter, (SliceNode) indices[0]);
        } else {
            var indexConverters = convertIndices(indices);
            checkTypes(varConverter.returnType()[0], indexConverters, valueConverter.returnType()[0]);
            bytes.addAll(varConverter.convert());
            finishIndex(bytes, storeBytes, valueConverter, indices);
        }
    }

    private void finishIndex(
            @NotNull BytecodeList bytes, @NotNull BytecodeList storeBytes,
            TestConverter valueConverter, TestNode[] indices
    ) {
        for (var indexParam : indices) {
            bytes.addAll(TestConverter.bytes(indexParam, info, 1));
        }
        bytes.addAll(valueConverter.convert());
        storeBytes.addFirst(Bytecode.STORE_SUBSCRIPT, indices.length);
    }

    private void finishSlice(
            @NotNull BytecodeList bytes, @NotNull BytecodeList storeBytes,
            TestConverter valueConverter, SliceNode index
    ) {
        bytes.addAll(valueConverter.convert());
        bytes.addAll(new SliceConverter(info, index).convert());
        storeBytes.addFirst(Bytecode.CALL_OP, OpSpTypeNode.SET_SLICE.ordinal(), 2);
    }

    private void assignTopToDot(
            @NotNull BytecodeList bytes, @NotNull DottedVariableNode variable, TypeObject valueType
    ) {
        var last = variable.getLast().getPostDot();
        if (last instanceof IndexNode) {
            assignTopToDotIndex(bytes, variable, valueType);
        } else if (last instanceof VariableNode) {
            assignTopToNormalDot(bytes, variable, valueType);
        } else {
            throw CompilerException.of("Cannot assign", variable);
        }
    }

    private void assignTopToNormalDot(
            @NotNull BytecodeList bytes, @NotNull DottedVariableNode variable, TypeObject valueType
    ) {
        var pair = DotConverter.exceptLast(info, variable, 1);
        var preDotConverter = pair.getKey();
        var needsMakeOption = checkAssign(preDotConverter, pair.getValue(), valueType, variable);
        bytes.addAll(preDotConverter.convert());
        bytes.add(Bytecode.SWAP_2);
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        var nameAssigned = pair.getValue();
        bytes.addFirst(Bytecode.STORE_ATTR, info.constIndex(LangConstant.of(nameAssigned)));
    }

    private void assignTopToDotIndex(
            @NotNull BytecodeList bytes, @NotNull DottedVariableNode variable, TypeObject valueType
    ) {
        var pair = DotConverter.exceptLastIndex(info, variable, 1);
        topToIndex(bytes, pair.getKey(), pair.getValue(), valueType);
    }

    @NotNull
    private TypeObject assignType(@NotNull TestConverter preDotConverter, String postDot, Lined node) {
        var preDotRet = preDotConverter.returnType()[0];
        return preDotRet.tryAttrType(node, postDot, info);
    }

    private boolean checkAssign(
            @NotNull TestConverter preDotConverter, @NotNull String name,
            TypeObject valueType, DottedVariableNode value
    ) {
        var preDotType = preDotConverter.returnType()[0];
        var dotType = preDotType.tryAttrType(node, name, info);
        if (!dotType.isSuperclass(valueType)) {
            if (OptionTypeObject.needsAndSuper(dotType, valueType)) {
                return true;
            }
            throw CompilerException.format(
                    "Cannot assign: '%s'.%s has type of '%s', which is not a superclass of '%s'",
                    node, preDotType.name(), name, dotType.name(), valueType.name()
            );
        } else if (!preDotType.canSetAttr(name, info) && !isConstructorException(preDotType, value)) {
            if (preDotType.makeMut().canSetAttr(name, info)) {
                throw CompilerException.of(
                        "Cannot assign to value that is not 'mut' or 'mref'", node
                );
            } else {
                throw CompilerException.format(
                        "Cannot assign: '%s'.%s does not support assignment",
                        node, preDotType.name(), name
                );
            }
        }
        return false;
    }

    private boolean isConstructorException(TypeObject preDotType, DottedVariableNode variableNode) {
        if (preDotIsSelf(variableNode)) {
            return info.accessHandler().isInConstructor(preDotType);
        } else {
            return false;
        }
    }

    private boolean preDotIsSelf(@NotNull DottedVariableNode variableNode) {
        if (variableNode.getPostDots().length != 1) {
            return false;
        }
        var preDot = variableNode.getPreDot();
        // 'self' is a reserved name, so this is enough to check
        return preDot instanceof VariableNode && ((VariableNode) preDot).getName().equals("self");
    }

    private static int quickNonVarCount(@NotNull AssignableNode[] names) {
        int count = 0;
        for (var name : names) {
            if (!(name instanceof VariableNode)) {
                count++;
                if (count > 1) {  // For its use (side-effect checks), there is no need to keep counting
                    return count;
                }
            }
        }
        return count;
    }
}
