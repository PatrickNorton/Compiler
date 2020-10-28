package main.java.converter;

import main.java.parser.DecrementNode;
import main.java.parser.IncDecNode;
import main.java.parser.IncrementNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IncrementDecrementConverter implements BaseConverter {
    private final IncDecNode node;
    private final CompilerInfo info;

    public IncrementDecrementConverter(CompilerInfo info, IncDecNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        boolean isDecrement = node instanceof DecrementNode;
        assert isDecrement ^ node instanceof IncrementNode;
        var converter = TestConverter.of(info, node.getVariable(), 1);
        if (!Builtins.INT.isSuperclass(converter.returnType()[0])) {
            throw CompilerException.format(
                    "TypeError: Object of type %s cannot be %s",
                    node.getLineInfo(), converter.returnType()[0].name(), isDecrement ? "decremented" : "incremented");
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.LOAD_CONST.value);
        int constIndex = info.addConstant(LangConstant.of(1));
        bytes.addAll(Util.shortToBytes((short) constIndex));
        bytes.add((isDecrement ? Bytecode.MINUS : Bytecode.PLUS).value);
        if (node.getVariable() instanceof VariableNode) {
            short varIndex = info.varIndex((VariableNode) node.getVariable());
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(varIndex));
        } else {
            throw CompilerInternalError.of("Non-variable in/decrement not yet implemented", node);
        }
        return bytes;
    }
}
