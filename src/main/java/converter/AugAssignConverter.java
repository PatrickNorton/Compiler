package main.java.converter;

import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.OpSpTypeNode;
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
        var name = node.getName();
        if (name instanceof VariableNode) {
            return convertVar(start);
        } else if (name instanceof DottedVariableNode) {
            return convertDot(start);
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
        var assignedConverter = dotValueConverter();
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var name = (DottedVariableNode) node.getName();
        var dotName = name.getPostDots()[name.getPostDots().length - 1];
        var strName = ((VariableNode) dotName.getPostDot()).getName();
        var converterReturn = assignedConverter.returnType()[0];
        var dotType = converterReturn.tryAttrType(node, strName, info);
        var returnInfo = dotType.operatorInfo(trueOp, info.accessLevel(converterReturn));
        checkInfo(returnInfo.orElse(null), dotType, valueConverter.returnType()[0]);
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.addAll(valueConverter.convert(start));
        bytes.add(OperatorConverter.BYTECODE_MAP.get(operator).value);
        bytes.add(Bytecode.STORE_ATTR.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(strName))));
        return bytes;
    }

    @NotNull
    private TestConverter dotValueConverter() {
        var value = node.getName();
        assert value instanceof DottedVariableNode;
        var dottedVar = (DottedVariableNode) value;
        if (dottedVar.getPostDots().length == 1) {
            return TestConverter.of(info, dottedVar.getPreDot(), 1);
        } else {
            throw CompilerTodoError.of(
                    "Augmented assignment does not work with dot chains longer than 1 yet", dottedVar
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
}
