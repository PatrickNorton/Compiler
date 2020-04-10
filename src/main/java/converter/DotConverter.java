package main.java.converter;

import main.java.parser.DottedVar;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.NameNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DotConverter implements TestConverter {
    private final DottedVariableNode node;
    private final CompilerInfo info;
    private final int retCount;

    public DotConverter(CompilerInfo info, DottedVariableNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var result = TestConverter.returnType(node.getPreDot(), info, 1)[0];
        for (var dot : node.getPostDots()) {
            result = dotReturnType(result, dot);
        }
        return new TypeObject[]{result};
    }

    @NotNull
    private TypeObject dotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        switch (dot.getDotPrefix()) {
            case "":
                return normalDotReturnType(result, dot);
            case "?":
                return optionalDotReturnType(result, dot);
            case "!!":
                return nonNullReturnType(result, dot);
            default:
                throw new RuntimeException("Unknown type of dot " + dot.getDotPrefix());
        }
    }

    @NotNull
    private TypeObject normalDotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().isEmpty() || !result.isSuperclass(Builtins.NULL_TYPE);
        var postDot = dot.getPostDot();
        if (postDot instanceof VariableNode) {
            return result.tryAttrType(postDot, ((VariableNode) postDot).getName(), info);
        } else if (postDot instanceof FunctionCallNode) {
            var caller = ((FunctionCallNode) postDot).getCaller();
            var attrType = result.tryAttrType(postDot, ((VariableNode) caller).getName(), info);
            return attrType.tryOperatorReturnType(postDot.getLineInfo(), OpSpTypeNode.CALL, info)[0];
        } else if (postDot instanceof SpecialOpNameNode) {
            var operator = ((SpecialOpNameNode) postDot).getOperator();
            var accessLevel = info.accessLevel(result);
            return result.operatorInfo(operator, accessLevel).toCallable();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private TypeObject optionalDotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("?");
        if (!result.isSuperclass(Builtins.NULL_TYPE)) {
            return normalDotReturnType(result, dot);
        }
        var postDot = dot.getPostDot();
        if (postDot instanceof VariableNode) {
            var retType = result.stripNull().tryAttrType(postDot, ((VariableNode) postDot).getName(), info);
            return TypeObject.optional(retType);
        } else if (postDot instanceof FunctionCallNode) {
            var caller = ((FunctionCallNode) postDot).getCaller();
            var attrType = result.stripNull().tryAttrType(postDot, ((VariableNode) caller).getName(), info);
            return TypeObject.optional(attrType.operatorReturnType(OpSpTypeNode.CALL, info)[0]);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    private TypeObject nonNullReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        var hasNull = result.isSuperclass(Builtins.NULL_TYPE);
        var bangType = hasNull ? result.stripNull() : result;
        return normalDotReturnType(bangType, dot);
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
                    break;
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
        assert dot.getDotPrefix().equals("?");  // TODO: Optimizations & warnings for non-null types
        var postDot = dot.getPostDot();
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.JUMP_NULL.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        convertPostDot(start, bytes, postDot);
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
    }

    private void convertNotNullDot(int start, @NotNull List<Byte> bytes, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("!!");  // TODO: Optimizations & warnings for non-null types
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
            bytes.remove(bytes.size() - 1);
            var op = ((SpecialOpNameNode) postDot).getOperator();
            bytes.add(Bytecode.LOAD_OP.value);
            bytes.addAll(Util.shortToBytes((short) op.ordinal()));
        } else {
            throw new UnsupportedOperationException("This kind of post-dot not yet supported");
        }
    }
}
