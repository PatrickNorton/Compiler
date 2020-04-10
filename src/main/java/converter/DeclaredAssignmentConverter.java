package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeclaredAssignmentConverter implements BaseConverter {
    private final CompilerInfo info;
    private final DeclaredAssignmentNode node;

    public DeclaredAssignmentConverter(CompilerInfo info, DeclaredAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        assert !node.isColon();
        if (node.getNames().length > 1) {
            throw new UnsupportedOperationException("Conversion of multiple-var assignment not implemented");
        }
        var value = node.getValues().get(0);
        var assigned = node.getTypes()[0];
        var converter = TestConverter.of(info, value, 1);
        var valueType = converter.returnType()[0];
        var rawType = assigned.getType();
        var nonConstAssignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
        var assignedType = node.getDescriptors().contains(DescriptorNode.MUT)
                ? nonConstAssignedType.makeMut() : nonConstAssignedType.makeConst();
        var assignedName = assigned.getVariable().getName();
        if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
            throw CompilerException.of("Illegal name " + assignedName, node);
        }
        if (!assignedType.isSuperclass(valueType)) {
            throw CompilerException.format(
                    "Object of type %s cannot be assigned to object of type %s",
                    node, valueType.name(), assignedType.name());
        }
        boolean isConst = !node.getDescriptors().contains(DescriptorNode.MUT);
        if (isConst && converter instanceof ConstantConverter) {
            var constant = ((ConstantConverter) converter).constant();
            info.addVariable(assignedName, assignedType, constant);
            return Collections.emptyList();
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        info.addVariable(assignedName, assignedType, isConst);
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(assignedName)));
        return bytes;
    }
}
