package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class VariantConverter implements TestConverter {
    private final CompilerInfo info;
    private final VariantCreationNode node;
    private final int retCount;

    public VariantConverter(CompilerInfo info, VariantCreationNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var unionConverter = TestConverter.of(info, node.getUnion(), 1);
        var retType = unionConverter.returnType()[0];
        var optionRet = new OptionTypeObject(retType.tryAttrType(node, node.getVariantName(), AccessLevel.PUBLIC));
        return new TypeObject[] {optionRet};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        assert retCount == 1 || retCount == 0;
        var unionConverter = TestConverter.of(info, node.getUnion(), 1);
        var retType = unionConverter.returnType()[0];
        assert retType instanceof TypeTypeObject;
        List<Byte> bytes = new ArrayList<>(unionConverter.convert(start));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getValue(), info, 1));
        bytes.add(Bytecode.MAKE_VARIANT.value);
        bytes.addAll(Util.shortToBytes((short) node.getVariantNo()));
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }
}
