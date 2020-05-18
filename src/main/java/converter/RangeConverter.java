package main.java.converter;

import main.java.parser.RangeLiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RangeConverter implements TestConverter {
    private final RangeLiteralNode node;
    private final CompilerInfo info;
    private final int retCount;

    public RangeConverter(CompilerInfo info, RangeLiteralNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.RANGE};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Range literal creation has unused result", node);
            return Collections.emptyList();
        }
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        var constant = info.constIndex(Builtins.constantOf("range"));
        bytes.addAll(Util.shortToBytes(constant));
        convertPortion(start, bytes, node.getStart(), 0);
        convertPortion(start, bytes, node.getEnd(), 0);
        convertPortion(start, bytes, node.getStep(), 1);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 3));
        return bytes;
    }

    private void convertPortion(int start, List<Byte> bytes, @NotNull TestNode node, int defaultVal) {
        if (!node.isEmpty()) {
            var converter = TestConverter.of(info, node, 1);
            if (!Builtins.INT.isSuperclass(converter.returnType()[0])) {
                throw CompilerException.format(
                        "TypeError: Type %s does not match required type %s",
                        node, converter.returnType()[0].name(), Builtins.INT.name()
                );
            }
            bytes.addAll(converter.convert(start));
        } else {
            var constIndex = info.constIndex(LangConstant.of(defaultVal));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(constIndex));
        }
    }
}
