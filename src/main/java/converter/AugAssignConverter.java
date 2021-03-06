package main.java.converter;

import main.java.parser.AugAssignTypeNode;
import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IndexNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AugAssignConverter implements BaseConverter {
    private final CompilerInfo info;
    private final AugmentedAssignmentNode node;

    public AugAssignConverter(CompilerInfo info, AugmentedAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (node.getOperator() == AugAssignTypeNode.NULL_COERCE) {
            return convertNullCoerce(start);
        }
        var name = node.getName();
        if (name instanceof VariableNode) {
            return convertVar(start);
        } else if (name instanceof DottedVariableNode) {
            var last = ((DottedVariableNode) name).getLast();
            if (last.getPostDot() instanceof VariableNode) {
                return convertDot(start);
            } else if (last.getPostDot() instanceof IndexNode) {
                return convertDotIndex(start);
            } else if (last.getPostDot() instanceof FunctionCallNode) {
                throw CompilerException.of("Augmented assignment does not work on function calls", name);
            } else {
                throw CompilerTodoError.of("Augmented assignment on non-standard dotted variables", name);
            }
        } else if (name instanceof IndexNode) {
            return convertIndex(start);
        } else if (name instanceof FunctionCallNode) {
            throw CompilerException.of("Augmented assignment does not work on function calls", name);
        } else {
            throw CompilerTodoError.of(
                    "Augmented assignment to non-variable or dotted variables not supported yet", name
            );
        }
    }

    @NotNull
    private List<Byte> convertVar(int start) {
        assert node.getName() instanceof VariableNode;
        var assignedConverter = TestConverter.of(info, node.getName(), 1);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var converterReturn = assignedConverter.returnType()[0];
        var operator = node.getOperator().operator;
        var opInfo = converterReturn.operatorInfo(OpSpTypeNode.translate(operator), info);
        checkInfo(opInfo.orElse(null), assignedConverter.returnType()[0], valueConverter.returnType()[0]);
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator).value);
        bytes.add(Bytecode.STORE.value);
        var variable = (VariableNode) node.getName();
        bytes.addAll(Util.shortToBytes(info.varIndex(variable)));
        return bytes;
    }

    @NotNull
    private List<Byte> convertDot(int start) {
        assert node.getName() instanceof DottedVariableNode;
        var operator = node.getOperator().operator;
        var trueOp = OpSpTypeNode.translate(operator);
        var name = (DottedVariableNode) node.getName();
        var pair = DotConverter.exceptLast(info, name, 1);
        var assignedConverter = pair.getKey();
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var strName = pair.getValue();
        var converterReturn = assignedConverter.returnType()[0];
        var dotType = converterReturn.tryAttrType(node, strName, info);
        var returnInfo = dotType.operatorInfo(trueOp, info.accessLevel(converterReturn));
        checkInfo(returnInfo.orElse(null), dotType, valueConverter.returnType()[0]);
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.LOAD_DOT.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(strName))));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator).value);
        bytes.add(Bytecode.STORE_ATTR.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(strName))));
        return bytes;
    }

    private List<Byte> convertDotIndex(int start) {
        assert node.getName() instanceof DottedVariableNode;
        var name = (DottedVariableNode) node.getName();
        var pair = DotConverter.exceptLastIndex(info, name, 1);
        var assignedConverter = pair.getKey();
        var indices = pair.getValue();
        return convertIndex(start, assignedConverter, indices);
    }

    private List<Byte> convertIndex(int start) {
        assert node.getName() instanceof IndexNode;
        var index = (IndexNode) node.getName();
        var assignedConverter = TestConverter.of(info, index.getVar(), 1);
        return convertIndex(start, assignedConverter, index.getIndices());
    }

    private List<Byte> convertIndex(int start, TestConverter assignedConverter, TestNode[] indices) {
        var operator = node.getOperator().operator;
        var trueOp = OpSpTypeNode.translate(operator);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var converterReturn = assignedConverter.returnType()[0];
        var attrInfo = converterReturn.tryOperatorInfo(node, OpSpTypeNode.GET_ATTR, info);
        converterReturn.tryOperatorInfo(node, OpSpTypeNode.SET_ATTR, info);
        var dotType = attrInfo.getReturns()[0];
        var returnInfo = dotType.operatorInfo(trueOp, info.accessLevel(converterReturn));
        checkInfo(returnInfo.orElse(null), dotType, valueConverter.returnType()[0]);
        var bytes = IndexConverter.convertDuplicate(start, assignedConverter, indices, info);
        bytes.addAll(Util.shortToBytes((short) indices.length));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator).value);
        bytes.add(Bytecode.STORE_SUBSCRIPT.value);
        bytes.addAll(Util.shortToBytes((short) indices.length));
        return bytes;
    }

    private List<Byte> convertNullCoerce(int start) {
        var name = node.getName();
        if (name instanceof VariableNode) {
            return convertNullCoerceVar(start);
        } else if (name instanceof DottedVariableNode) {
            return convertNullCoerceDot(start);
        } else if (name instanceof IndexNode) {
            return convertNullCoerceIndex(start);
        } else {
            throw CompilerInternalError.of("Unknown type for null-coerce assignment", node);
        }
    }

    private List<Byte> convertNullCoerceVar(int start) {
        var variable = (VariableNode) node.getName();
        if (info.variableIsImmutable(variable.getName())) {
            throw CompilerException.of("Cannot assign to immutable variable", node);
        }
        var assignedConverter = TestConverter.of(info, variable, 1);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var variableType = assignedConverter.returnType()[0];
        var valueType = valueConverter.returnType()[0];
        if (!(variableType instanceof OptionTypeObject)) {
            throw coerceError(node, variableType);
        }
        boolean needsMakeOption = needsMakeOption((OptionTypeObject) variableType, valueType);
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.add(Bytecode.JUMP_NN.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(valueConverter.convert(start));
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(variable)));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        return bytes;
    }

    private List<Byte> convertNullCoerceDot(int start) {
        var name = (DottedVariableNode) node.getName();
        if (name.getLast().getPostDot() instanceof VariableNode) {
            return convertNullCoerceDotVar(start);
        } else if (name.getLast().getPostDot() instanceof IndexNode) {
            return convertNullCoerceDottedIndex(start);
        } else {
            throw CompilerTodoError.of("??= on other dotted variables", name.getLast());
        }
    }

    private List<Byte> convertNullCoerceDotVar(int start) {
        var name = (DottedVariableNode) node.getName();
        var pair = DotConverter.exceptLast(info, name, 1);
        var preDotConverter = pair.getKey();
        var strName = pair.getValue();
        var retType = preDotConverter.returnType()[0];
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var valueType = valueConverter.returnType()[0];
        var variableType = retType.tryAttrType(name.getLast(), strName, info);
        if (!(variableType instanceof OptionTypeObject)) {
            throw coerceError(node, variableType);
        }
        var needsMakeOption = needsMakeOption((OptionTypeObject) variableType, valueType);
        var constIndex = info.constIndex(LangConstant.of(strName));
        List<Byte> bytes = new ArrayList<>(preDotConverter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.LOAD_DOT.value);
        bytes.addAll(Util.shortToBytes(constIndex));
        bytes.add(Bytecode.JUMP_NN.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(valueConverter.convert(start));
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        bytes.add(Bytecode.STORE_ATTR.value);
        bytes.addAll(Util.shortToBytes(constIndex));
        bytes.add(Bytecode.JUMP.value);
        int jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        bytes.add(Bytecode.POP_TOP.value);
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump2);
        return bytes;
    }

    private List<Byte> convertNullCoerceIndex(int start) {
        var name = (IndexNode) node.getName();
        var preDotConverter = TestConverter.of(info, name.getVar(), 1);
        var postDotConverters = convertersOf(name.getIndices());
        return convertNullIndex(start, preDotConverter, postDotConverters);
    }

    private List<Byte> convertNullCoerceDottedIndex(int start) {
        var name = (DottedVariableNode) node.getName();
        var converterPair = DotConverter.exceptLastIndex(info, name, 1);
        var preDotConverter = converterPair.getKey();
        var postDotConverters = convertersOf(converterPair.getValue());
        return convertNullIndex(start, preDotConverter, postDotConverters);
    }

    private List<Byte> convertNullIndex(int start, TestConverter preDotConverter, TestConverter... postDotConverters) {
        var name = node.getName();
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var preDotType = preDotConverter.returnType()[0];
        var variableType = preDotType.tryOperatorReturnType(name, OpSpTypeNode.GET_ATTR, info)[0];
        preDotType.tryOperatorInfo(name, OpSpTypeNode.SET_ATTR, info);
        var valueType = valueConverter.returnType()[0];
        if (!(variableType instanceof OptionTypeObject)) {
            throw coerceError(node, variableType);
        }
        var needsMakeOption = needsMakeOption((OptionTypeObject) variableType, valueType);
        List<Byte> bytes = new ArrayList<>(preDotConverter.convert(start));
        for (var postDot : postDotConverters) {
            bytes.addAll(postDot.convert(start + bytes.size()));
        }
        if (postDotConverters.length == 1) {
            bytes.add(Bytecode.DUP_TOP_2.value);
        } else {
            bytes.add(Bytecode.DUP_TOP_N.value);
            bytes.addAll(Util.shortToBytes((short) (postDotConverters.length + 1)));
        }
        bytes.add(Bytecode.LOAD_SUBSCRIPT.value);
        bytes.addAll(Util.shortToBytes((short) postDotConverters.length));
        bytes.add(Bytecode.JUMP_NN.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(valueConverter.convert(start));
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION.value);
        }
        bytes.add(Bytecode.STORE_SUBSCRIPT.value);
        bytes.addAll(Util.shortToBytes((short) (postDotConverters.length + 1)));
        bytes.add(Bytecode.JUMP.value);
        int jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        for (int i = 0; i < postDotConverters.length + 1; i++) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump2);
        return bytes;
    }

    private boolean needsMakeOption(OptionTypeObject variableType, TypeObject valueType) {
        if (variableType.isSuperclass(valueType)) {
            return false;
        } else if (variableType.getOptionVal().isSuperclass(valueType)) {
            return true;
        } else {
            throw CompilerException.format(
                    "Cannot assign: Expected instance of '%s', got '%s'",
                    node, variableType.getOptionVal().name(), valueType.name()
            );
        }
    }

    private void checkInfo(FunctionInfo fnInfo, TypeObject assignedReturn, TypeObject valueReturn) {
        if (fnInfo == null) {
            throw CompilerException.format(
                    "Value of type '%s' does not have an overloaded '%s' operator",
                    node, assignedReturn.name(), node.getOperator().operator.name
            );
        } else {
            var lineInfo = node.getValue().getLineInfo();
            var argument = new Argument("", valueReturn, false, lineInfo);
            if (!fnInfo.matches(argument)) {
                throw CompilerException.format(
                        "'%s'.operator %s cannot be called with type '%s'",
                        lineInfo, assignedReturn.name(), node.getOperator().operator.name, valueReturn.name()
                );
            }
            var returnType = fnInfo.getReturns()[0];
            if (!assignedReturn.isSuperclass(returnType)) {
                throw CompilerException.format(
                        "Value of type %s has a return type of '%s', which is incompatible with the type of '%s'",
                        node, assignedReturn.name(), returnType.name(), node.getName()
                );
            }
        }
    }

    private TestConverter[] convertersOf(TestNode[] nodes) {
        var result = new TestConverter[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            result[i] = TestConverter.of(info, nodes[i], 1);
        }
        return result;
    }

    private static CompilerException coerceError(Lined node, TypeObject valueType) {
        return CompilerException.format(
                "??= only works on an optional variable, not one of type '%s'", node, valueType.name()
        );
    }
}
