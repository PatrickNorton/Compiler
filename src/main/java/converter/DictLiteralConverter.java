package main.java.converter;

import main.java.parser.DictLiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DictLiteralConverter implements TestConverter {
    private final DictLiteralNode node;
    private final CompilerInfo info;
    private final int retCount;
    private final TypeObject[] expected;

    public DictLiteralConverter(CompilerInfo info, DictLiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expected = null;
    }

    public DictLiteralConverter(CompilerInfo info, DictLiteralNode node, int retCount, TypeObject[] expected) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expected = expected;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        if (node.getKeys().length == 0) {
            assert node.getValues().length == 0;
            if (expected == null) {
                throw CompilerException.of("Cannot deduce type of dict literal", node);
            }
            var generics = expected[0].getGenerics();
            return new TypeObject[] {Builtins.dict().generify(node, generics.toArray(new TypeObject[0])).makeMut()};
        } else {
            var keyType = returnTypes(node.getKeys());
            var valType = returnTypes(node.getValues());
            return new TypeObject[] {Builtins.dict().generify(keyType, valType).makeMut()};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        BytecodeList bytes = new BytecodeList();
        assert node.getKeys().length == node.getValues().length;
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warn("Unnecessary dict creation", WarningType.UNUSED, info, node);
            for (var pair : node.pairs()) {
                bytes.addAll(BaseConverter.bytes(pair.getKey(), info));
                bytes.addAll(BaseConverter.bytes(pair.getValue(), info));
            }
        } else {
            if (retCount != 1) {
                throw CompilerException.format("Dict literal only returns 1 value, not %d", node, retCount);
            }
            var keyType = returnTypes(node.getKeys());
            var valType = returnTypes(node.getValues());
            for (var pair : node.pairs()) {
                bytes.addAll(TestConverter.bytesMaybeOption(pair.getKey(), info, 1, keyType));
                bytes.addAll(TestConverter.bytesMaybeOption(pair.getValue(), info, 1, valType));
            }
            bytes.add(Bytecode.DICT_CREATE, node.size());
        }
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes(@NotNull TestNode[] args) {
        var result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = TestConverter.returnType(args[i], info, 1)[0];
        }
        return args.length == 0 ? Builtins.object() : TypeObject.union(result);
    }
}
