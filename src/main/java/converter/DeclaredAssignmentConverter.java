package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.util.Zipper;
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
        if (node.getValues().size() == 1 && node.getNames().length > 1) {
            return convertSingle(start);
        } else {
            return convertMultiple(start);
        }
    }

    @NotNull
    private List<Byte> convertMultiple(int start) {
        var types = node.getTypes();
        var values = node.getValues();
        if (types.length != values.size()) {
            throw CompilerException.format(
                    "Multiple returns are not supported in = statements " +
                            "with more than one operand (got %d variables and %d expressions)",
                    node, types.length, values.size()
            );
        }
        var isStatic = node.getDescriptors().contains(DescriptorNode.STATIC);
        List<Byte> bytes = new ArrayList<>();
        int fillPos = addStatic(bytes, isStatic);
        for (var pair : new Zipper<>(List.of(types), values)) {
            var assigned = pair.getKey();
            var valuePair = pair.getValue();
            var value = valuePair.getKey();
            if (!valuePair.getValue().isEmpty()) {
                throw CompilerException.of("Varargs not yet supported here", assigned);
            }
            var converter = TestConverter.of(info, value, 1);
            var valueType = converter.returnType()[0];
            var rawType = assigned.getType();
            var nonConstAssignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
            var mutability = MutableType.fromNullable(node.getMutability().orElse(null));
            var assignedType = mutability.isConstType()
                    ? nonConstAssignedType.makeConst() : nonConstAssignedType.makeMut();
            var assignedName = assigned.getVariable().getName();
            if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
                throw CompilerException.of("Illegal name " + assignedName, node);
            }
            boolean needsMakeOption = checkTypes(assignedType, valueType);
            boolean isConst = mutability.isConstRef();
            if (isConst && converter instanceof ConstantConverter) {
                var constant = ((ConstantConverter) converter).constant();
                info.checkDefinition(assignedName, node);
                if (needsMakeOption) {
                    short constIndex = info.addConstant(constant);
                    var trueConst = new OptionConstant(valueType, constIndex);
                    info.addVariable(assignedName, assignedType, trueConst, node);
                } else {
                    info.addVariable(assignedName, assignedType, constant, node);
                }
                return Collections.emptyList();
            }
            bytes.addAll(OptionTypeObject.maybeWrapBytes(converter.convert(start), needsMakeOption));
            finishAssignment(bytes, isStatic, assignedType, assignedName, isConst);
        }
        if (isStatic) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), fillPos);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertSingle(int start) {
        var values = node.getValues();
        var types = node.getTypes();
        assert values.size() == 1;
        var value = node.getValues().get(0);
        var valueConverter = TestConverter.of(info, value, types.length);
        var valueTypes = valueConverter.returnType();
        var isStatic = node.getDescriptors().contains(DescriptorNode.STATIC);
        List<Byte> bytes = new ArrayList<>();
        int fillPos = addStatic(bytes, isStatic);
        bytes.addAll(valueConverter.convert(start + bytes.size()));
        // Iterate backward b/c variables are in reversed order on the stack
        // B/c this is *declared* assignment, we know this to be side-effect free, so this is safe
        for (int i = types.length - 1; i >= 0; i--) {
            var assigned = types[i];
            var valueType = valueTypes[i];
            var rawType = assigned.getType();
            var nonConstAssignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
            var mutability = MutableType.fromNullable(node.getMutability().orElse(null));
            var assignedType = mutability.isConstType()
                    ? nonConstAssignedType.makeConst() : nonConstAssignedType.makeMut();
            var assignedName = assigned.getVariable().getName();
            if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
                throw CompilerException.of("Illegal name " + assignedName, node);
            }
            boolean needsMakeOption = checkTypes(assignedType, valueType);
            boolean isConst = mutability.isConstRef();
            if (needsMakeOption) {
                bytes.add(Bytecode.MAKE_OPTION.value);
            }
            finishAssignment(bytes, isStatic, assignedType, assignedName, isConst);
        }
        if (isStatic) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), fillPos);
        }
        return bytes;
    }

    private int addStatic(List<Byte> bytes, boolean isStatic) {
        if (isStatic) {
            bytes.add(Bytecode.DO_STATIC.value);
            int fillPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
            return fillPos;
        } else {
            return -1;
        }
    }

    private boolean checkTypes(@NotNull TypeObject assignedType, @NotNull TypeObject valueType) {
        if (!assignedType.isSuperclass(valueType)) {
            if (OptionTypeObject.needsMakeOption(assignedType, valueType)) {
                return true;
            } else {
                throw CompilerException.format(
                        "Object of type %s cannot be assigned to object of type %s",
                        node, valueType.name(), assignedType.name());
            }
        } else {
            return false;
        }
    }

    private void finishAssignment(
            @NotNull List<Byte> bytes, boolean isStatic, TypeObject assignedType, String assignedName, boolean isConst
    ) {
        info.checkDefinition(assignedName, node);
        var index = isStatic
                ? info.addStaticVar(assignedName, assignedType, isConst, node)
                : info.addVariable(assignedName, assignedType, isConst, node);
        bytes.add(isStatic ? Bytecode.STORE_STATIC.value : Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(index));
    }
}
