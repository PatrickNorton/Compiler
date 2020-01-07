package main.java.converter;

import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.ParserException;

import java.util.ArrayList;
import java.util.Collections;
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
        var converter = TestConverter.of(info, value);
        var valueType = converter.returnType();
        var assignedType = info.getType(assigned.getType());
        if (!valueType.isSubclass(assignedType)) {
            throw ParserException.of(String.format(
                    "Object of type %s cannot be assigned to object of type %s",
                    converter.returnType(), assigned.getType()), node);
        }
        if (converter instanceof ConstantConverter) {
            var constant = ((ConstantConverter) converter).constant();
            info.addVariable(assigned.getVariable().getName(), info.getType(assigned.getType()), constant);
            return Collections.emptyList();
        } else {
            info.addVariable(assigned.getVariable().getName(), info.getType(assigned.getType()));
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes((short) info.varIndex(assigned.getVariable().getName())));
            return bytes;
        }
    }
}
