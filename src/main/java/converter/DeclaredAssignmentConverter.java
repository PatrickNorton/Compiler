package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeclaredAssignmentConverter implements BaseConverter {
    private CompilerInfo info;
    private DeclaredAssignmentNode node;

    public DeclaredAssignmentConverter(CompilerInfo info, DeclaredAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (node.getNames().length > 1) {
            throw new UnsupportedOperationException("Conversion of multiple-var assignment not implemented");
        }
        var value = node.getValues().get(0);
        var assigned = node.getTypes()[0];
        var converter = TestConverter.of(info, value);
        var valueType = converter.returnType();
        var rawType = assigned.getType();
        var assignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
        var assignedName = assigned.getVariable().getName();
        if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
            throw CompilerException.of("Illegal name " + assignedName, node);
        }
        if (!valueType.isSubclass(assignedType)) {
            throw CompilerException.format(
                    "Object of type %s cannot be assigned to object of type %s",
                    node, valueType, assignedType);
        }
        if (converter instanceof ConstantConverter) {
            var constant = ((ConstantConverter) converter).constant();
            info.addVariable(assignedName, assignedType, constant);
            return Collections.emptyList();
        } else {
            info.addVariable(assignedName, assignedType);
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes((short) info.varIndex(assignedName)));
            return bytes;
        }
    }
}
