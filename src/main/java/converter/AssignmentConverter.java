package main.java.converter;

import main.java.parser.AssignmentNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IndexNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AssignmentConverter implements BaseConverter {
    private CompilerInfo info;
    private AssignmentNode node;

    public AssignmentConverter(CompilerInfo info, AssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        assert !node.isColon() : "No colon assignment yet";
        var names = node.getNames();
        var values = node.getValues();
        assert names.length == values.size() : "Multiple returns not supported yet";
        List<Byte> assignBytes = new ArrayList<>();
        List<Byte> storeBytes = new ArrayList<>(names.length * Bytecode.STORE.size());
        for (int i = 0; i < names.length; i++) {
            var name = names[i];
            var value = values.get(i);
            var valueConverter = TestConverter.of(info, value, 1);
            if (name instanceof VariableNode) {
                assignToVariable(assignBytes, storeBytes, start, (VariableNode) name, valueConverter);
            } else if (name instanceof IndexNode) {
                assignToIndex(assignBytes, storeBytes, start, (IndexNode) name, valueConverter);
            } else if (name instanceof DottedVariableNode) {
                assignToDot(assignBytes, storeBytes, start, (DottedVariableNode) name, valueConverter);
            } else {
                throw new UnsupportedOperationException("Assignment to this type not yet supported");
            }
        }
        assignBytes.addAll(storeBytes);
        return assignBytes;
    }

    private void assignToVariable(@NotNull List<Byte> bytes, List<Byte> storeBytes, int start,
                                  @NotNull VariableNode variable, @NotNull TestConverter valueConverter) {
        var valueType = valueConverter.returnType();
        if (info.varIsUndefined(variable.getName())) {
            throw CompilerException.format("Attempted to assign to undefined name %s",
                    variable, variable.getName());
        }
        var varType = info.getType(variable.getName());
        if (!valueType.isSubclass(varType)) {
            throw CompilerException.format("Cannot assign type %s to variable of type %s",
                    node, valueType, varType);
        }
        bytes.addAll(valueConverter.convert(start));
        storeBytes.add(0, Bytecode.STORE.value);
        storeBytes.addAll(1, Util.shortToBytes(info.varIndex(variable.getName())));
    }

    private void assignToIndex(@NotNull List<Byte> bytes, List<Byte> storeBytes, int start,
                               @NotNull IndexNode variable, @NotNull TestConverter valueConverter) {
        var indices = variable.getIndices();
        // FIXME: Check types
        bytes.addAll(TestConverter.bytes(start, variable.getVar(), info, 1));
        for (var indexParam : indices) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), indexParam, info, 1));
        }
        bytes.addAll(valueConverter.convert(start + bytes.size()));
        storeBytes.add(0, Bytecode.STORE_SUBSCRIPT.value);
        storeBytes.addAll(1, Util.shortToBytes((short) indices.length));
    }

    private void assignToDot(@NotNull List<Byte> bytes, @NotNull List<Byte> storeBytes, int start,
                             @NotNull DottedVariableNode variable, @NotNull TestConverter valueConverter) {
        assert variable.getPostDots().length == 1;
        bytes.addAll(TestConverter.bytes(start + bytes.size(), variable.getPreDot(), info, 1));
        bytes.addAll(valueConverter.convert(start + bytes.size()));
        storeBytes.add(0, Bytecode.STORE_ATTR.value);
        var nameAssigned = (VariableNode) variable.getPostDots()[0].getPostDot();
        storeBytes.addAll(1, Util.shortToBytes(info.constIndex(LangConstant.of(nameAssigned.getName()))));
    }
}
