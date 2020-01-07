package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;

import java.util.ArrayList;
import java.util.List;

public class DeclaredAssignmentConverter implements BaseConverter {
    private CompilerInfo info;
    private DeclaredAssignmentNode node;

    public DeclaredAssignmentConverter(CompilerInfo info, DeclaredAssignmentNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public List<Byte> convert(int start) {
        if (node.getNames().length > 1) {
            throw new UnsupportedOperationException("Conversion of multiple-var assignment not implemented");
        }
        var value = node.getValues().get(0);
        var assigned = node.getTypes()[0];
        info.addVariable(assigned.getVariable().getName(), info.getType(assigned.getType()));
        List<Byte> bytes = new ArrayList<>(BaseConverter.bytes(start, value, info));
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes((short) info.varIndex(assigned.getVariable().getName())));
        return bytes;
    }
}
