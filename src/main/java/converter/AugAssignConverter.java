package main.java.converter;

import main.java.parser.AugAssignTypeNode;
import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IndexNode;
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
            if (last.getPostDot() instanceof IndexNode) {
                return convertDotIndex(start);
            } else {
                return convertDot(start);
            }
        } else if (name instanceof IndexNode) {
            return convertIndex(start);
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
        if (!(node.getName() instanceof VariableNode)) {
            throw CompilerTodoError.of("??= for non-variables", node);
        }
        var variable = (VariableNode) node.getName();
        if (info.variableIsImmutable(variable.getName())) {
            throw CompilerException.of("Cannot assign to immutable variable", node);
        }
        var assignedConverter = TestConverter.of(info, variable, 1);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var valueType = assignedConverter.returnType()[0];
        if (!(valueType instanceof OptionTypeObject)) {
            throw CompilerException.format(
                    "??= only works on an optional variable, not one of type '%s'", node, valueType
            );
        }
        var innerType = ((OptionTypeObject) valueType).getOptionVal();
        var variableType = valueConverter.returnType()[0];
        boolean needsMakeOption;
        if (valueType.isSuperclass(variableType)) {
            needsMakeOption = false;
        } else if (innerType.isSuperclass(variableType)) {
            needsMakeOption = true;
        } else {
            throw CompilerException.format(
                    "Cannot assign: Expected instance of '%s', got '%s'",
                    node, innerType.name(), variableType.name()
            );
        }
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
}
