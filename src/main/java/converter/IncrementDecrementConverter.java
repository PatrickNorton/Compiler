package main.java.converter;

import main.java.parser.DecrementNode;
import main.java.parser.IncDecNode;
import main.java.parser.IncrementNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class IncrementDecrementConverter implements BaseConverter {
    private boolean isDecrement;
    private IncDecNode node;
    private CompilerInfo info;

    public IncrementDecrementConverter(boolean isDecrement, CompilerInfo info, IncDecNode node) {
        this.node = node;
        this.info = info;
        this.isDecrement = isDecrement;
    }

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        assert isDecrement ? node instanceof DecrementNode : node instanceof IncrementNode;
        var converter = TestConverter.of(info, node.getVariable());
        if (!converter.returnType().isSubclass(Builtins.INT)) {
            throw CompilerException.format(
                    "TypeError: Object of type %s cannot be incremented",
                    node.getLineInfo(), converter.returnType());
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.LOAD_CONST.value);
        int constIndex = info.addConstant(LangConstant.of(1));
        bytes.addAll(Util.shortToBytes((short) constIndex));
        bytes.add((isDecrement ? Bytecode.MINUS : Bytecode.PLUS).value);
        if (node.getVariable() instanceof VariableNode) {
            int varIndex = info.varIndex(((VariableNode) node.getVariable()).getName());
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes((short) varIndex));
        } else {
            throw CompilerInternalError.of("Non-variable increment not yet implemented", node);
        }
        return bytes;
    }
}
