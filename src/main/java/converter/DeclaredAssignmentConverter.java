package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypedVariableNode;
import org.jetbrains.annotations.NotNull;

public final class DeclaredAssignmentConverter implements BaseConverter {
    private final CompilerInfo info;
    private final DeclaredAssignmentNode node;

    public DeclaredAssignmentConverter(CompilerInfo info, DeclaredAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (node.isColon()) {
            throw CompilerException.of(":= is only allowed for defining class members", node);
        }
        if (isSingle()) {
            return convertSingle();
        } else {
            return convertMultiple();
        }
    }

    private boolean isSingle() {
        return node.getValues().size() == 1 && node.getNames().length > 1
                && node.getValues().getVararg(0).isEmpty();
    }

    @NotNull
    private BytecodeList convertMultiple() {
        var types = node.getTypes();
        var values = node.getValues();
        var isStatic = node.getDescriptors().contains(DescriptorNode.STATIC);
        var bytes = new BytecodeList();
        var staticLbl = addStatic(bytes, isStatic);
        var mutability = MutableType.fromNullable(node.getMutability().orElse(null));
        boolean isConst = mutability.isConstRef();
        int tupleCount = 0;
        for (int i = 0; i < values.size(); i++) {
            switch (values.getVararg(i)) {
                case "" -> convertNoTuple(bytes, types[i - tupleCount], isStatic, values.get(i), mutability, isConst);
                case "*" -> tupleCount += convertTuple(bytes, isStatic, mutability, isConst, tupleCount, i);
                case "**" -> throw CompilerException.of(
                        "Cannot unpack dictionaries in declared assignment", values.get(i)
                );
                default -> throw CompilerInternalError.format(
                        "Invalid splat type '%s'", values.get(i), values.getVararg(i)
                );
            }
        }
        if (types.length != values.size() + tupleCount) {
            throw CompilerException.format(
                    "Multiple returns are not supported in = statements " +
                            "with more than one operand (got %d variables and %d expressions)",
                    node, types.length, values.size()
            );
        }
        if (isStatic) {
            bytes.addLabel(staticLbl);
        }
        return bytes;
    }

    private void convertNoTuple(
            BytecodeList bytes, @NotNull TypedVariableNode assigned, boolean isStatic,
            TestNode value, MutableType mutability, boolean isConst
    ) {
        var rawType = assigned.getType();
        var converter = getConverter(value, rawType);
        var valueType = converter.returnType()[0];
        var assignedType = getAssigned(valueType, rawType, mutability);
        var assignedName = assigned.getVariable().getName();
        checkName(assignedName);
        boolean needsMakeOption = checkTypes(assignedType, valueType);
        var constValue = converter.constantReturn();
        if (isConst && constValue.isPresent()) {
            var constant = constValue.orElseThrow();
            addConstant(valueType, assignedType, assignedName, needsMakeOption, constant);
        } else {
            bytes.addAll(OptionTypeObject.maybeWrapBytes(converter.convert(), needsMakeOption));
            finishAssignment(bytes, isStatic, assignedType, assignedName, isConst, assigned);
        }
    }

    private int convertTuple(
            BytecodeList bytes, boolean isStatic, MutableType mutability,
            boolean isConst, int tupleCount, int i
    ) {
        var types = node.getTypes();
        var values = node.getValues();
        var converter = TestConverter.of(info, values.get(i), 1);
        var retType = converter.returnType()[0];
        int argC = checkTuple(retType, values.get(i));
        var valueTypes = expandZeroTuple(converter);
        assert argC == valueTypes.length;
        bytes.addAll(converter.convert());
        bytes.add(Bytecode.UNPACK_TUPLE);
        for (int j = argC - 1; j >= 0; j--) {
            var valueType = valueTypes[j];
            var assigned = types[i - tupleCount + j];
            var rawType = assigned.getType();
            var assignedType = getAssigned(valueType, rawType, mutability);
            var assignedName = assigned.getVariable().getName();
            checkName(assignedName);
            boolean needsMakeOption = checkTypes(assignedType, valueType);
            if (needsMakeOption) {
                bytes.add(Bytecode.MAKE_OPTION);
            }
            finishAssignment(bytes, isStatic, assignedType, assignedName, isConst, assigned);
        }
        return argC - 1;
    }

    private void checkName(String name) {
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.of("Illegal name " + name, node);
        }
    }

    private int checkTuple(TypeObject retType, Lined node) {
        if (retType instanceof TupleType) {
            return retType.getGenerics().size();
        } else if (retType.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
            throw CompilerException.of(
                    "Vararg on declared assignment does not work for iterables, only tuples", node
            );
        } else {
            throw CompilerException.format("Vararg expected tuple, got type '%s'", node, retType.name());
        }
    }

    private TestConverter getConverter(TestNode value, TypeLikeNode rawType) {
        return rawType.isDecided()
                    ? TestConverter.of(info, value, 1, info.getType(rawType))
                    : TestConverter.of(info, value, 1);
    }

    private void addConstant(TypeObject valueType, TypeObject assignedType, String assignedName,
                             boolean needsMakeOption, LangConstant constant) {
        info.checkDefinition(assignedName, node);
        if (needsMakeOption) {
            short constIndex = info.addConstant(constant);
            var trueConst = new OptionConstant(valueType, constIndex);
            info.addVariable(assignedName, assignedType, trueConst, node);
        } else {
            info.addVariable(assignedName, assignedType, constant, node);
        }
    }

    @NotNull
    private BytecodeList convertSingle() {
        var values = node.getValues();
        var types = node.getTypes();
        assert values.size() == 1;
        var value = node.getValues().get(0);
        var valueConverter = TestConverter.of(info, value, types.length);
        var valueTypes = valueConverter.returnType();
        var isStatic = node.getDescriptors().contains(DescriptorNode.STATIC);
        BytecodeList bytes = new BytecodeList();
        var staticLabel = addStatic(bytes, isStatic);
        bytes.addAll(valueConverter.convert());
        var mutability = MutableType.fromNullable(node.getMutability().orElse(null));
        boolean isConst = mutability.isConstRef();
        // Iterate backward b/c variables are in reversed order on the stack
        // B/c this is *declared* assignment, we know this to be side-effect free, so this is safe
        // We probably don't even *need* to build up the stack here, but I'm slightly wary that might
        // actually cause order-of-execution issues
        for (int i = types.length - 1; i >= 0; i--) {
            var assigned = types[i];
            var valueType = valueTypes[i];
            var assignedType = getAssigned(valueType, assigned.getType(), mutability);
            var assignedName = assigned.getVariable().getName();
            if (Builtins.FORBIDDEN_NAMES.contains(assignedName)) {
                throw CompilerException.of("Illegal name " + assignedName, node);
            }
            boolean needsMakeOption = checkTypes(assignedType, valueType);
            if (needsMakeOption) {
                bytes.add(Bytecode.MAKE_OPTION);
            }
            finishAssignment(bytes, isStatic, assignedType, assignedName, isConst, assigned);
        }
        if (isStatic) {
            assert staticLabel != null;
            bytes.addLabel(staticLabel);
        }
        return bytes;
    }

    private TypeObject getAssigned(TypeObject valueType, TypeLikeNode rawType, MutableType mutability) {
        var nonConstAssignedType = rawType.isDecided() ? info.getType(rawType) : valueType;
        return mutability.isConstType() ? nonConstAssignedType.makeConst() : nonConstAssignedType.makeMut();
    }

    private Label addStatic(BytecodeList bytes, boolean isStatic) {
        if (isStatic) {
            var label = info.newJumpLabel();
            bytes.add(Bytecode.DO_STATIC, label);
            return label;
        } else {
            return null;
        }
    }

    private boolean checkTypes(@NotNull TypeObject assignedType, @NotNull TypeObject valueType) {
        if (!assignedType.isSuperclass(valueType)) {
            if (OptionTypeObject.needsAndSuper(assignedType, valueType)) {
                return true;
            } else {
                throw CompilerException.format(
                        "Object of type '%s' cannot be assigned to object of type '%s'",
                        node, valueType.name(), assignedType.name());
            }
        } else {
            return false;
        }
    }

    private void finishAssignment(
            @NotNull BytecodeList bytes, boolean isStatic, TypeObject assignedType,
            String assignedName, boolean isConst, Lined lineInfo
    ) {
        info.checkDefinition(assignedName, node);
        if (isStatic && !isConst) {
            throw mutStaticException(lineInfo);
        }
        var index = isStatic
                ? info.addStaticVar(assignedName, assignedType, true, node)
                : info.addVariable(assignedName, assignedType, isConst, node);
        bytes.add(isStatic ? Bytecode.STORE_STATIC : Bytecode.STORE, index);
    }

    private CompilerException mutStaticException(Lined lineInfo) {
        return CompilerException.of(
                "Local static variable may not be 'mut' or 'mref'", lineInfo
        );
    }

    private TypeObject[] expandZeroTuple(TestConverter valueConverter) {
        var valT = valueConverter.returnType();
        if (node.getValues().getVararg(0).isEmpty()) {
            return valT;
        } else if (valT[0] instanceof TupleType) {
            return valT[0].getGenerics().toArray(new TypeObject[0]);
        } else {
            throw CompilerException.format(
                    "Vararg used on non-tuple argument (returned type '%s')",
                    node.getValues().get(0), valT[0].name()
            );
        }
    }
}
