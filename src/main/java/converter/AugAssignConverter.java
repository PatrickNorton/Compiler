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
        var assignedConverter = TestConverter.of(info, node.getName());
        var valueConverter = TestConverter.of(info, node.getValue());
        var returnType = assignedConverter.returnType().operatorReturnType(node.getOperator().operator);
        if (returnType == null) {
            throw CompilerException.format("Value of type %s does not have an overloaded %s operator",
                    node, assignedConverter.returnType(), node.getOperator().operator.name);
        } else if (!returnType.isSubclass(assignedConverter.returnType())) {
            throw CompilerException.format(
                    "Value of type %s has a return type of %s, which is incompatible with the type of %s",
                    node, assignedConverter.returnType(), returnType, node.getName());
        }
        List<Byte> bytes = new ArrayList<>(assignedConverter.convert(start));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(Bytecode.STORE.value);
        var variable = (VariableNode) node.getName();  // TODO: Add assignment for other types
        bytes.addAll(Util.shortToBytes((short) info.varIndex(variable.getName())));
        return bytes;
    }
}
