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
        if (node.isColon()) {
            throw CompilerException.of(":= is only allowed for defining class members", node);
        }
        if (node.getNames().length > 1) {
            throw new UnsupportedOperationException("Conversion of multiple-var assignment not implemented");
        }
        var value = node.getValues().get(0);
        var assigned = node.getTypes()[0];
        var converter = TestConverter.of(info, value, 1);
        var valueType = converter.returnType()[0];
        var rawType = assigned.getType();
        var nonConstAssignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
        var descriptors = node.getDescriptors();
        var assignedType = descriptors.contains(DescriptorNode.MUT)
                ? nonConstAssignedType.makeMut() : nonConstAssignedType.makeConst();
        var assignedName = assigned.getVariable().getName();
        if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
            throw CompilerException.of("Illegal name " + assignedName, node);
        }
        boolean needsMakeOption;
        if (!assignedType.isSuperclass(valueType)) {
            if (OptionTypeObject.needsMakeOption(assignedType, valueType)) {
                needsMakeOption = true;
            } else {
                throw CompilerException.format(
                        "Object of type %s cannot be assigned to object of type %s",
                        node, valueType.name(), assignedType.name());
            }
        } else {
            needsMakeOption = false;
        }
        boolean isConst = !descriptors.contains(DescriptorNode.MUT)
                && !descriptors.contains(DescriptorNode.MREF);
        if (isConst && converter instanceof ConstantConverter) {
            var constant = ((ConstantConverter) converter).constant();
            info.checkDefinition(assignedName, node);
            info.addVariable(assignedName, assignedType, constant, node);
            return Collections.emptyList();
        }
        boolean isStatic = descriptors.contains(DescriptorNode.STATIC);
        List<Byte> bytes = new ArrayList<>();
        int fillPos;
        if (isStatic) {
            bytes.add(Bytecode.DO_STATIC.value);
            fillPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        } else {
            fillPos = -1;
        }
        bytes.addAll(OptionTypeObject.maybeWrapBytes(converter.convert(start), needsMakeOption));
        info.checkDefinition(assignedName, node);
        var index = isStatic
                ? info.addStaticVar(assignedName, assignedType, isConst, node)
                : info.addVariable(assignedName, assignedType, isConst, node);
        bytes.add(isStatic ? Bytecode.STORE_STATIC.value : Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(index));
        if (isStatic) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), fillPos);
        }
        return bytes;
    }
}
