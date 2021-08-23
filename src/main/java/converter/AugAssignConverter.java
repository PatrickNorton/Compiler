package main.java.converter;

import main.java.parser.AugAssignTypeNode;
import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.EscapedOperatorNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IndexNode;
import main.java.parser.Lined;
import main.java.parser.NameNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

public final class AugAssignConverter implements BaseConverter {
    private final CompilerInfo info;
    private final AugmentedAssignmentNode node;

    public AugAssignConverter(CompilerInfo info, AugmentedAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (node.getOperator() == AugAssignTypeNode.NULL_COERCE) {
            return convertNullCoerce();
        }
        var name = removeIllegal(node.getName());
        if (name instanceof VariableNode) {
            return convertVar();
        } else if (name instanceof DottedVariableNode) {
            var last = ((DottedVariableNode) name).getLast();
            var postDot = removeIllegal(last.getPostDot());
            if (postDot instanceof VariableNode) {
                return convertDot();
            } else if (postDot instanceof IndexNode) {
                return convertDotIndex();
            } else if (postDot instanceof DottedVariableNode) {
                throw CompilerInternalError.of(
                        "Dotted variables should not have dotted variables as post-dots", name
                );
            } else {
                throw CompilerInternalError.format(
                        "Type not filtered by removeIllegal(): %s", name, postDot.getClass()
                );
            }
        } else if (name instanceof IndexNode) {
            return convertIndex();
        } else {
            throw CompilerInternalError.format(
                    "Type not filtered by removeIllegal(): %s", name, name.getClass()
            );
        }
    }

    @NotNull
    private BytecodeList convertVar() {
        assert node.getName() instanceof VariableNode;
        var assignedConverter = TestConverter.of(info, node.getName(), 1);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var converterReturn = assignedConverter.returnType()[0];
        var operator = node.getOperator().operator;
        var opInfo = converterReturn.operatorInfo(OpSpTypeNode.translate(operator), info);
        checkInfo(opInfo.orElse(null), assignedConverter.returnType()[0], valueConverter.returnType()[0]);
        var bytes = new BytecodeList(assignedConverter.convert());
        bytes.addAll(valueConverter.convert());
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator));
        var variable = (VariableNode) node.getName();
        bytes.add(Bytecode.STORE, info.varIndex(variable));
        return bytes;
    }

    @NotNull
    private BytecodeList convertDot() {
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
        var bytes = new BytecodeList(assignedConverter.convert());
        bytes.add(Bytecode.DUP_TOP);
        bytes.add(Bytecode.LOAD_DOT, info.constIndex(LangConstant.of(strName)));
        bytes.addAll(valueConverter.convert());
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator));
        bytes.add(Bytecode.STORE_ATTR, info.constIndex(LangConstant.of(strName)));
        return bytes;
    }

    @NotNull
    private BytecodeList convertDotIndex() {
        assert node.getName() instanceof DottedVariableNode;
        var name = (DottedVariableNode) node.getName();
        var pair = DotConverter.exceptLastIndex(info, name, 1);
        var assignedConverter = pair.getKey();
        var indices = pair.getValue();
        return convertIndex(assignedConverter, indices);
    }

    @NotNull
    private BytecodeList convertIndex() {
        assert node.getName() instanceof IndexNode;
        var index = (IndexNode) node.getName();
        var assignedConverter = TestConverter.of(info, index.getVar(), 1);
        return convertIndex(assignedConverter, index.getIndices());
    }

    @NotNull
    private BytecodeList convertIndex(@NotNull TestConverter assignedConverter, TestNode[] indices) {
        var operator = node.getOperator().operator;
        var trueOp = OpSpTypeNode.translate(operator);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var converterReturn = assignedConverter.returnType()[0];
        var attrInfo = converterReturn.tryOperatorInfo(node, OpSpTypeNode.GET_ATTR, info);
        converterReturn.tryOperatorInfo(node, OpSpTypeNode.SET_ATTR, info);
        var dotType = attrInfo.getReturns()[0];
        var returnInfo = dotType.operatorInfo(trueOp, info.accessLevel(converterReturn));
        checkInfo(returnInfo.orElse(null), dotType, valueConverter.returnType()[0]);
        var bytes = IndexConverter.convertDuplicate(assignedConverter, indices, info, indices.length);
        bytes.addAll(valueConverter.convert());
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator));
        bytes.add(Bytecode.STORE_SUBSCRIPT, indices.length);
        return bytes;
    }

    private BytecodeList convertNullCoerce() {
        var name = removeIllegal(node.getName());
        if (name instanceof VariableNode) {
            return convertNullCoerceVar();
        } else if (name instanceof DottedVariableNode) {
            return convertNullCoerceDot();
        } else if (name instanceof IndexNode) {
            return convertNullCoerceIndex();
        } else {
            throw CompilerInternalError.format(
                    "Post-dot type not filtered by removeIllegal(): %s", name, name.getClass()
            );
        }
    }

    @NotNull
    private BytecodeList convertNullCoerceVar() {
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
        var bytes = new BytecodeList(assignedConverter.convert());
        var label = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_NN, label);
        bytes.addAll(valueConverter.convert());
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        bytes.add(Bytecode.STORE, info.varIndex(variable));
        bytes.addLabel(label);
        return bytes;
    }

    private BytecodeList convertNullCoerceDot() {
        var name = (DottedVariableNode) node.getName();
        var postDot = removeIllegal(name.getLast().getPostDot());
        if (postDot instanceof VariableNode) {
            return convertNullCoerceDotVar();
        } else if (postDot instanceof IndexNode) {
            return convertNullCoerceDottedIndex();
        } else if (postDot instanceof DottedVariableNode) {
            throw CompilerInternalError.format("Dotted variables should not be part of a post-dot", postDot);
        } else {
            throw CompilerInternalError.format(
                    "Post-dot type not filtered by removeIllegal(): %s", postDot, postDot.getClass()
            );
        }
    }

    @NotNull
    private BytecodeList convertNullCoerceDotVar() {
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
        var bytes = new BytecodeList(preDotConverter.convert());
        bytes.add(Bytecode.DUP_TOP);
        bytes.add(Bytecode.LOAD_DOT, constIndex);
        var label = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_NN, label);
        bytes.addAll(valueConverter.convert());
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        bytes.add(Bytecode.STORE_ATTR, constIndex);
        var label2 = info.newJumpLabel();
        bytes.add(Bytecode.JUMP, label2);
        bytes.addLabel(label);
        bytes.add(Bytecode.POP_TOP);
        bytes.addLabel(label2);
        return bytes;
    }

    @NotNull
    private BytecodeList convertNullCoerceIndex() {
        var name = (IndexNode) node.getName();
        var preDotConverter = TestConverter.of(info, name.getVar(), 1);
        var postDotConverters = convertersOf(name.getIndices());
        return convertNullIndex(preDotConverter, postDotConverters);
    }

    @NotNull
    private BytecodeList convertNullCoerceDottedIndex() {
        var name = (DottedVariableNode) node.getName();
        var converterPair = DotConverter.exceptLastIndex(info, name, 1);
        var preDotConverter = converterPair.getKey();
        var postDotConverters = convertersOf(converterPair.getValue());
        return convertNullIndex(preDotConverter, postDotConverters);
    }

    @NotNull
    private BytecodeList convertNullIndex(@NotNull TestConverter preDotConverter, TestConverter... postDotConverters) {
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
        var bytes = new BytecodeList(preDotConverter.convert());
        for (var postDot : postDotConverters) {
            bytes.addAll(postDot.convert());
        }
        if (postDotConverters.length == 1) {
            bytes.add(Bytecode.DUP_TOP_2);
        } else {
            bytes.add(Bytecode.DUP_TOP_N, postDotConverters.length + 1);
        }
        bytes.add(Bytecode.LOAD_SUBSCRIPT, postDotConverters.length);
        var label = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_NN, label);
        bytes.addAll(valueConverter.convert());
        if (needsMakeOption) {
            bytes.add(Bytecode.MAKE_OPTION);
        }
        bytes.add(Bytecode.STORE_SUBSCRIPT, postDotConverters.length + 1);
        var label2 = info.newJumpLabel();
        bytes.add(Bytecode.JUMP, label2);
        bytes.addLabel(label);
        for (int i = 0; i < postDotConverters.length + 1; i++) {
            bytes.add(Bytecode.POP_TOP);
        }
        bytes.addLabel(label2);
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

    private NameNode removeIllegal(NameNode name) {
        if (name instanceof VariableNode || name instanceof IndexNode || name instanceof DottedVariableNode) {
            return name;
        } else if (name instanceof FunctionCallNode) {
            throw CompilerException.of(illegalMessage("function calls"), node);
        } else if (name instanceof SpecialOpNameNode) {
            throw CompilerException.of(illegalMessage("operator names"), node);
        } else if (name instanceof EscapedOperatorNode) {
            throw CompilerException.of(illegalMessage("escaped operators"), node);
        } else {
            throw CompilerInternalError.format("Unknown type of NameNode: %s", name, name.getClass());
        }
    }

    private TestConverter[] convertersOf(TestNode[] nodes) {
        var result = new TestConverter[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            result[i] = TestConverter.of(info, nodes[i], 1);
        }
        return result;
    }

    private static String illegalMessage(String value) {
        return String.format("Augmented assignment does not work on %s", value);
    }

    private static CompilerException coerceError(Lined node, TypeObject valueType) {
        return CompilerException.format(
                "??= only works on an optional variable, not one of type '%s'", node, valueType.name()
        );
    }
}
