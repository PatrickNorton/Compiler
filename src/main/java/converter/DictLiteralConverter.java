package main.java.converter;

import main.java.parser.DictLiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DictLiteralConverter implements TestConverter {
    private final DictLiteralNode node;
    private final CompilerInfo info;
    private final int retCount;

    public DictLiteralConverter(CompilerInfo info, DictLiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var keyType = returnTypes(node.getKeys());
        var valType = returnTypes(node.getValues());
        return new TypeObject[] {Builtins.DICT.generify(keyType, valType)};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warn("Unnecessary dict creation", node);
            for (var pair : node.pairs()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), pair.getKey(), info));
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), pair.getValue(), info));
            }
        } else {
            assert retCount == 1;
            var keyType = returnTypes(node.getKeys());
            var valType = returnTypes(node.getValues());
            for (var pair : node.pairs()) {
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), pair.getKey(), info, 1, keyType));
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), pair.getValue(), info, 1, valType));
            }
            bytes.add(Bytecode.DICT_CREATE.value);
            bytes.addAll(Util.shortToBytes((short) node.size()));
        }
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes(@NotNull TestNode[] args) {
        var result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = TestConverter.returnType(args[i], info, 1)[0];
        }
        return args.length == 0 ? Builtins.OBJECT : TypeObject.union(result);
    }
}
