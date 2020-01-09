package main.java.converter;

import main.java.parser.FunctionCallNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallConverter implements TestConverter {
    private CompilerInfo info;
    private FunctionCallNode node;

    public FunctionCallConverter(CompilerInfo info, FunctionCallNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    public List<Byte> convert(int start) {
        var callConverter = TestConverter.of(info, node.getCaller());
        ensureTypesMatch(callConverter.returnType());
        List<Byte> bytes = new ArrayList<>(callConverter.convert(start));
        for (var value : node.getParameters()) {
            bytes.addAll(BaseConverter.bytes(start, value, info));
        }
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) node.getParameters().length));
        return bytes;
    }

    @Override
    public TypeObject returnType() {
        return info.getType("");
    }

    private void ensureTypesMatch(@NotNull TypeObject callerType) {
        if (!callerType.isSubclass(Builtins.CALLABLE)) {
            throw CompilerException.of("Cannot call " + node.getCaller(), node);
        }
    }
}
