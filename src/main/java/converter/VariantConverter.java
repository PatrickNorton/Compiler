package main.java.converter;

import org.jetbrains.annotations.NotNull;

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
        var retType = unionConverter.returnType()[0].getGenerics().get(0);
        return new TypeObject[] {retType.makeMut()};
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (retCount == 0) {
            CompilerWarning.warn("Unused variant access", WarningType.UNUSED, info, node);
        } else if (retCount > 1) {
            throw CompilerException.format("Union variant can only return one value, not %d", node, retCount);
        }
        var unionConverter = TestConverter.of(info, node.getUnion(), 1);
        var retType = unionConverter.returnType()[0];
        assert retType instanceof TypeTypeObject;
        var bytes = new BytecodeList(unionConverter.convert());
        bytes.addAll(TestConverter.bytes(node.getValue(), info, 1));
        bytes.add(Bytecode.MAKE_VARIANT, node.getVariantNo());
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }
}
