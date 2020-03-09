package main.java.converter;

import main.java.parser.DottedVar;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.NameNode;
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

    @NotNull
    @Override
    public TypeObject[] returnType() {  // TODO: Non-null dots, etc.
        var result = TestConverter.returnType(node.getPreDot(), info, 1)[0];
        for (var dot : node.getPostDots()) {
            result = result.attrType(dot.getPostDot().toString());  // FIXME: Function calls &c.
        }
        return new TypeObject[]{result};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getPreDot(), info, 1));
        for (var dot : node.getPostDots()) {
            switch (dot.getDotPrefix()) {
                case "":
                    convertNormal(start, bytes, dot);
                    break;
                case "?":
                    convertNullDot(start, bytes, dot);
                    break;
                case "!!":
                    convertNotNullDot(start, bytes, dot);
                default:
                    throw new RuntimeException("Unknown value for dot prefix");
            }
        }
        return bytes;
    }

    private void convertNormal(int start, @NotNull List<Byte> bytes, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().isEmpty();
        var postDot = dot.getPostDot();
        convertPostDot(start, bytes, postDot);
    }

    private void convertNullDot(int start, @NotNull List<Byte> bytes, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("?");
        var postDot = dot.getPostDot();
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.JUMP_NULL.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        convertPostDot(start, bytes, postDot);
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
    }

    private void convertNotNullDot(int start, @NotNull List<Byte> bytes, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("!!");
        var postDot = dot.getPostDot();
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.JUMP_NN.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.POP_TOP.value);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("str"))));  // TODO: Error type
        bytes.add(Bytecode.LOAD_CONST.value);
        var message = String.format("Value %s asserted non-null, was null", postDot);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        convertPostDot(start, bytes, postDot);
    }

    private void convertPostDot(int start, @NotNull List<Byte> bytes, @NotNull NameNode postDot) {
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
}
