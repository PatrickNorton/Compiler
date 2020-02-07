package main.java.converter;

import main.java.parser.DottedVariableNode;
import main.java.parser.NameNode;
import main.java.parser.VariableNode;

import java.util.ArrayList;
import java.util.List;

public class DotConverter implements TestConverter {
    private DottedVariableNode node;
    private CompilerInfo info;

    public DotConverter(CompilerInfo info, DottedVariableNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public TypeObject returnType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getPreDot(), info, 1));
        for (var postDot : node.getPostDots()) {
            bytes.add(Bytecode.LOAD_DOT.value);
            assert postDot instanceof NameNode;
            if (postDot instanceof VariableNode) {
                var name = LangConstant.of(((VariableNode) postDot).getName());
                bytes.addAll(Util.shortToBytes((short) info.constIndex(name)));
            } else {
                throw new UnsupportedOperationException("This kind of post-dot not yet supported");
            }
        }
        return bytes;
    }
}
