package main.java.converter;

import main.java.parser.RangeLiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RangeConverter implements TestConverter {
    private RangeLiteralNode node;
    private CompilerInfo info;

    public RangeConverter(CompilerInfo info, RangeLiteralNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public TypeObject returnType() {
        return Builtins.RANGE;
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        convertPortion(start, bytes, node.getStart());
        convertPortion(start, bytes, node.getEnd());
        convertPortion(start, bytes, node.getStep());
        bytes.add(Bytecode.LOAD_CONST.value);
        var constant = info.constIndex(Builtins.constantOf("range"));
        bytes.addAll(Util.shortToBytes((short) constant));
        return bytes;
    }

    private void convertPortion(int start, List<Byte> bytes, @NotNull TestNode node) {
        if (!node.isEmpty()) {
            var converter = TestConverter.of(info, node);
            if (!converter.returnType().isSubclass(Builtins.INT)) {
                throw CompilerException.format(
                        "TypeError: Type %s does not match required type %s",
                        node, converter.returnType(), Builtins.INT
                );
            }
            bytes.addAll(converter.convert(start));
        }
    }
}
