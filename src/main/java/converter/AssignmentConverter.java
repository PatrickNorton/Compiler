package main.java.converter;

import main.java.parser.AssignmentNode;
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
        assert !node.isColon();
        if (node.getNames().length > 1) {
            throw new UnsupportedOperationException("Assignment to multiple values not yet supported");
        }
        assert node.getValues().size() == node.getNames().length;
        var name = node.getNames()[0];
        var value = node.getValues().get(0);
        var valueConverter = TestConverter.of(info, value, 1);
        var valueType = valueConverter.returnType();
        List<Byte> bytes = new ArrayList<>();
        if (name instanceof VariableNode) {
            var variable = (VariableNode) name;
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
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes((short) info.varIndex(variable.getName())));
        } else if (name instanceof IndexNode) {
            var variable = (IndexNode) name;
            var indices = variable.getIndices();
            // FIXME: Check types
            bytes.addAll(TestConverter.bytes(start, variable.getVar(), info, 1));
            for (var indexParam : indices) {
                bytes.addAll(TestConverter.bytes(start + bytes.size(), indexParam, info, 1));
            }
            bytes.addAll(valueConverter.convert(start + bytes.size()));
            bytes.add(Bytecode.STORE_SUBSCRIPT.value);
            bytes.addAll(Util.shortToBytes((short) indices.length));
        } else {
            throw new UnsupportedOperationException("Assignment to this type not yet supported");
        }
        return bytes;
    }
}
