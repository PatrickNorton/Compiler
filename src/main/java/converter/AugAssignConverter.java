package main.java.converter;

import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AugAssignConverter implements BaseConverter {
    private CompilerInfo info;
    private AugmentedAssignmentNode node;

    public AugAssignConverter(CompilerInfo info, AugmentedAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        var assignedConverter = TestConverter.of(info, node.getName(), 1);
        var valueConverter = TestConverter.of(info, node.getValue(), 1);
        var returnType = assignedConverter.returnType()[0].operatorReturnType(node.getOperator().operator)[0];
        if (returnType == null) {
            throw CompilerException.format("Value of type %s does not have an overloaded %s operator",
                    node, assignedConverter.returnType(), node.getOperator().operator.name);
        } else if (!returnType.isSuperclass(assignedConverter.returnType()[0])) {
            throw CompilerException.format(
                    "Value of type %s has a return type of %s, which is incompatible with the type of %s",
                    node, assignedConverter.returnType(), returnType, node.getName());
        }
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(OperatorConverter.BYTECODE_MAP.get(node.getOperator().operator).value);
        bytes.add(Bytecode.STORE.value);
        var variable = (VariableNode) node.getName();  // TODO: Add assignment for other types
        bytes.addAll(Util.shortToBytes(info.varIndex(variable.getName())));
        return bytes;
    }
}
