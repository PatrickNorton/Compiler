package main.java.converter;

import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DotConverter implements TestConverter {
    private DottedVariableNode node;
    private CompilerInfo info;
    private int retCount;

    public DotConverter(CompilerInfo info, DottedVariableNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public TypeObject returnType() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getPreDot(), info, 1));
        for (var dot : node.getPostDots()) {
            assert dot.getDotPrefix().isEmpty();
            var postDot = dot.getPostDot();
            bytes.add(Bytecode.LOAD_DOT.value);
            if (postDot instanceof VariableNode) {
                var name = LangConstant.of(((VariableNode) postDot).getName());
                bytes.addAll(Util.shortToBytes(info.constIndex(name)));
            } else if (postDot instanceof FunctionCallNode) {
                var caller = ((FunctionCallNode) postDot).getCaller();
                var name = LangConstant.of(((VariableNode) caller).getName());
                bytes.addAll(Util.shortToBytes(info.constIndex(name)));
                var callConverter = new FunctionCallConverter(info, (FunctionCallNode) postDot, retCount);
                callConverter.convertCall(bytes, start);
            } else if (postDot instanceof SpecialOpNameNode) {
                var op = ((SpecialOpNameNode) postDot).getOperator();
                bytes.add(Bytecode.LOAD_OP.value);
                bytes.addAll(Util.shortToBytes((short) op.ordinal()));
            } else {
                throw new UnsupportedOperationException("This kind of post-dot not yet supported");
            }
        }
        return bytes;
    }
}
