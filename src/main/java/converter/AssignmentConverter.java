package main.java.converter;

import main.java.parser.AssignableNode;
import main.java.parser.AssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IndexNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
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
    public List<Byte> convert(int start) {
        if (node.isColon()) {
            throw CompilerException.of("Colon assignment is not supported outside of class definitions", node);
        }
        if (node.getNames().length > 1 && node.getValues().size() == 1) {
            return assignSingleVariable(start);
        } else {
            return assignMultipleVariable(start);
        }
    }

    @NotNull
    private List<Byte> assignMultipleVariable(int start) {
        var names = node.getNames();
        var values = node.getValues();
        if (names.length != values.size()) {
            throw CompilerException.format(
                    "Multiple returns are not supported in = statements " +
                            "with more than one operand (expected %d, got %d)",
                    node, names.length, values.size()
            );
        }
        List<Byte> assignBytes = new ArrayList<>();
        List<Byte> storeBytes = new ArrayList<>(names.length * Bytecode.STORE.size());
        for (int i = 0; i < names.length; i++) {
            var name = names[i];
            var value = values.get(i);
            var valueConverter = TestConverter.of(info, value, 1);
            if (name instanceof VariableNode) {
                assignToVariable(assignBytes, storeBytes, start, (VariableNode) name, valueConverter);
            } else if (name instanceof IndexNode) {
                assignToIndex(assignBytes, storeBytes, start, (IndexNode) name, valueConverter);
            } else if (name instanceof DottedVariableNode) {
                assignToDot(assignBytes, storeBytes, start, (DottedVariableNode) name, value);
            } else {
                throw CompilerException.of("Assignment must be to a variable, index, or dotted variable", node);
            }
        }
        assignBytes.addAll(storeBytes);
        return assignBytes;
    }

    @NotNull
    private List<Byte> assignSingleVariable(int start) {
        assert node.getValues().size() == 1;
        var names = node.getNames();
        var value = node.getValues().get(0);
        var valueConverter = TestConverter.of(info, value, node.getNames().length);
        var retTypes = valueConverter.returnType();
        List<Byte> bytes = new ArrayList<>(valueConverter.convert(start));
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
                assignTop(bytes, start, retTypes[i], names[i]);
            }
        } else {
            // Have to swap everything (sigh)
            for (int i = 0; i < names.length; i++) {
                bringToTop(bytes, names.length - i - 1);
                assignTop(bytes, start, retTypes[i], names[i]);
            }
        }
        return bytes;
    }

    private void assignTop(List<Byte> bytes, int start, TypeObject retType, AssignableNode name) {
        if (name instanceof VariableNode) {
            assignTopToVariable(bytes, (VariableNode) name, retType);
        } else if (name instanceof IndexNode) {
            assignTopToIndex(bytes, start, (IndexNode) name, retType);
        } else if (name instanceof DottedVariableNode) {
            assignTopToDot(bytes, start, (DottedVariableNode) name, retType);
        } else {
            throw CompilerException.of("Assignment must be to a variable, index, or dotted variable", node);
        }
    }

    private void assignTopToVariable(List<Byte> bytes, @NotNull VariableNode variable, TypeObject valueType) {
        var name = variable.getName();
        checkDef(name, variable);
        var varType = info.getType(name);
        if (!varType.isSuperclass(valueType)) {
            if (!OptionTypeObject.needsMakeOption(varType, valueType)) {
                throw CompilerException.format("Cannot assign value of type '%s' to variable of type '%s'",
                        node, valueType.name(), varType.name());
            } else {
                bytes.add(Bytecode.MAKE_OPTION.value);
            }
        }
        bytes.add(0, Bytecode.STORE.value);
        bytes.addAll(1, Util.shortToBytes(info.varIndex(variable)));
    }

    private void assignToVariable(@NotNull List<Byte> bytes, List<Byte> storeBytes, int start,
                                  @NotNull VariableNode variable, @NotNull TestConverter valueConverter) {
        var valueType = valueConverter.returnType()[0];
        var name = variable.getName();
        checkDef(name, variable);
        var varType = info.getType(name);
        if (!varType.isSuperclass(valueType)) {
            if (!OptionTypeObject.needsMakeOption(varType, valueType)) {
                throw CompilerException.format("Cannot assign value of type '%s' to variable of type '%s'",
                        node, valueType.name(), varType.name());
            } else {
                bytes.addAll(OptionTypeObject.wrapBytes(valueConverter.convert(start + bytes.size())));
            }
        } else {
            bytes.addAll(valueConverter.convert(start + bytes.size()));
        }
        storeBytes.add(0, Bytecode.STORE.value);
        storeBytes.addAll(1, Util.shortToBytes(info.varIndex(variable)));
    }

    private void checkDef(String name, VariableNode variable) {
        if (info.varIsUndefined(name)) {
            throw CompilerException.format("Attempted to assign to undefined name %s", variable, name);
        }
        if (info.variableIsConstant(name)) {
            throw CompilerException.format("Cannot assign to const variable %s", variable, name);
        }
    }

    private void assignTopToIndex(
            @NotNull List<Byte> bytes, int start, @NotNull IndexNode variable, TypeObject valueType
    ) {
        var indices = variable.getIndices();
        var varConverter = TestConverter.of(info, variable.getVar(), 1);
        var indexConverters = convertIndices(indices);
        checkTypes(varConverter.returnType()[0], indexConverters, valueType);
        bytes.addAll(varConverter.convert(start + bytes.size()));
        for (var indexParam : indexConverters) {
            bytes.addAll(indexParam.convert(start + bytes.size()));
        }
        bringToTop(bytes, indices.length + 1);
        bytes.add(0, Bytecode.STORE_SUBSCRIPT.value);
        bytes.addAll(1, Util.shortToBytes((short) indices.length));
    }

    private static void bringToTop(List<Byte> bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2.value);
                return;
            case 2:
                bytes.add(Bytecode.SWAP_3.value);
                return;
            default:
                bytes.add(Bytecode.SWAP_N.value);
                bytes.addAll(Util.shortToBytes((short) (distFromTop + 1)));
        }
    }

    private void assignToIndex(@NotNull List<Byte> bytes, List<Byte> storeBytes, int start,
                               @NotNull IndexNode variable, @NotNull TestConverter valueConverter) {
        var indices = variable.getIndices();
        var varConverter = TestConverter.of(info, variable.getVar(), 1);
        var indexConverters = convertIndices(indices);
        checkTypes(varConverter.returnType()[0], indexConverters, valueConverter.returnType()[0]);
        bytes.addAll(TestConverter.bytes(start, variable.getVar(), info, 1));
        for (var indexParam : indices) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), indexParam, info, 1));
        }
        bytes.addAll(valueConverter.convert(start + bytes.size()));
        storeBytes.add(0, Bytecode.STORE_SUBSCRIPT.value);
        storeBytes.addAll(1, Util.shortToBytes((short) indices.length));
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
        var opInfo = varType.operatorInfo(OpSpTypeNode.SET_ATTR, info.accessLevel(varType));
        if (opInfo.isEmpty()) {
            throw CompilerException.format(
                    "Cannot assign variable to index (object of type '%s' has no operator []=)",
                    node, varType.name()
            );
        }
        if (!opInfo.orElseThrow().matches(indexTypes.toArray(new Argument[0]))) {
            throw CompilerException.format(
                    "Cannot assign variable to index: '%s'.operator []= does not match the given types", node
            );
        }
    }

    private void assignToDot(@NotNull List<Byte> bytes, @NotNull List<Byte> storeBytes, int start,
                             @NotNull DottedVariableNode variable, @NotNull TestNode value) {
        var pair = DotConverter.exceptLast(info, variable, 1);
        var preDotConverter = pair.getKey();
        var assignedType = assignType(preDotConverter, pair.getValue(), value);
        var valueConverter = TestConverter.of(info, value, 1, assignedType);
        var valueType = valueConverter.returnType()[0];
        var needsMakeOption = checkAssign(preDotConverter, variable, valueType);
        bytes.addAll(preDotConverter.convert(start + bytes.size()));
        bytes.addAll(OptionTypeObject.maybeWrapBytes(valueConverter.convert(start + bytes.size()), needsMakeOption));
        storeBytes.add(0, Bytecode.STORE_ATTR.value);
        var nameAssigned = (VariableNode) variable.getPostDots()[0].getPostDot();
        storeBytes.addAll(1, Util.shortToBytes(info.constIndex(LangConstant.of(nameAssigned.getName()))));
    }

    private void assignTopToDot(
            @NotNull List<Byte> bytes, int start, @NotNull DottedVariableNode variable, TypeObject valueType
    ) {
        assert variable.getPostDots().length == 1 : "Deeper-than-1-dot assignment not implemented";
        var preDotConverter = TestConverter.of(info, variable.getPreDot(), 1);
        var needsMakeOption = checkAssign(preDotConverter, variable, valueType);
        bytes.addAll(preDotConverter.convert(start + bytes.size()));
        bytes.add(Bytecode.SWAP_2.value);
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        bytes.add(0, Bytecode.STORE_ATTR.value);
        var nameAssigned = (VariableNode) variable.getPostDots()[0].getPostDot();
        bytes.addAll(1, Util.shortToBytes(info.constIndex(LangConstant.of(nameAssigned.getName()))));
    }

    @NotNull
    private TypeObject assignType(@NotNull TestConverter preDotConverter, String postDot, Lined node) {
        var preDotRet = preDotConverter.returnType()[0];
        return preDotRet.tryAttrType(node, postDot, info);
    }

    private boolean checkAssign(
            @NotNull TestConverter preDotConverter, @NotNull DottedVariableNode variable, TypeObject valueType
    ) {
        assert variable.getPostDots()[variable.getPostDots().length - 1].getPostDot() instanceof VariableNode;
        var dotType = TestConverter.returnType(variable, info, 1)[0];
        var preDotType = preDotConverter.returnType()[0];
        if (!dotType.isSuperclass(valueType)) {
            if (OptionTypeObject.needsMakeOption(dotType, valueType)
                    && OptionTypeObject.superWithOption(dotType, valueType)) {
                return true;
            }
            var postDots = variable.getPostDots();
            var nameAssigned = (VariableNode) postDots[postDots.length - 1].getPostDot();
            throw CompilerException.format(
                    "Cannot assign: '%s'.%s has type of '%s', which is not a superclass of '%s'",
                    node, preDotType.name(), nameAssigned.getName(), dotType.name(), valueType.name()
            );
        } else {
            var postDots = variable.getPostDots();
            var postDot = (VariableNode) postDots[postDots.length - 1].getPostDot();
            if (!preDotType.canSetAttr(postDot.getName(), info) && !isConstructorException(preDotType, variable)) {
                if (preDotType.makeMut().canSetAttr(postDot.getName(), info)) {
                    throw CompilerException.of(
                            "Cannot assign to value that is not 'mut' or 'final'", node
                    );
                } else {
                    throw CompilerException.format(
                            "Cannot assign: '%s'.%s does not support assignment",
                            node, preDotType.name(), postDot.getName()
                    );
                }
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
