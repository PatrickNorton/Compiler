package main.java.converter;

import main.java.parser.LiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class LiteralConverter implements TestConverter {
    private final LiteralNode node;
    private final CompilerInfo info;
    private final int retCount;

    public LiteralConverter(CompilerInfo info, LiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var mainType = node.getBraceType().equals("[") ? Builtins.LIST : Builtins.SET;
        return new TypeObject[]{mainType.generify(returnTypes(node.getBuilders()))};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warnf("Unnecessary %s creation", node, node.getBraceType().equals("[") ? "list" : "set");
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), value, info));
            }
        } else {
            assert retCount == 1;
            var retType = returnTypes(node.getBuilders());
            boolean isList = node.getBraceType().equals("[");
            for (var value : node.getBuilders()) {
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
            }
            bytes.add((isList ? Bytecode.LIST_CREATE : Bytecode.SET_CREATE).value);
            bytes.addAll(Util.shortToBytes((short) node.getBuilders().length));
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
