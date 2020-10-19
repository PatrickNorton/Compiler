package main.java.converter;

import main.java.parser.DottedVar;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IndexNode;
import main.java.parser.NameNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
    public static Pair<TestConverter, String> exceptLast(
            CompilerInfo info, @NotNull DottedVariableNode node, int retCount
    ) {
        var postDots = node.getPostDots();
        if (!(postDots[postDots.length - 1].getPostDot() instanceof VariableNode)) {
            throw CompilerTodoError.of(
                    "DotConverter.exceptLast does not work where the last dot is an index or function call",
                    postDots[postDots.length - 1]
            );
        }
        if (postDots.length == 1) {
            var converter = TestConverter.of(info, node.getPreDot(), retCount);
            var postDot = (VariableNode) postDots[0].getPostDot();
            return Pair.of(converter, postDot.getName());
        } else {
            var newPostDots = Arrays.copyOf(postDots, postDots.length - 1);
            var newNode = new DottedVariableNode(node.getPreDot(), newPostDots);
            var converter = TestConverter.of(info, newNode, retCount);
            var postDot = (VariableNode) postDots[postDots.length - 1].getPostDot();
            return Pair.of(converter, postDot.getName());
        }
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
            var opInfo = attrType.tryOperatorInfo(postDot, OpSpTypeNode.CALL, info);
            var retTypes = opInfo.getReturns();
            if (retTypes.length == 0) {
                throw CompilerException.of("Function does not return a value", dot);
            }
            var params = ((FunctionCallNode) postDot).getParameters();
            var trueRet = FunctionCallConverter.generifyReturns(opInfo, info, params, postDot, retCount);
            return trueRet[0];
        } else if (postDot instanceof SpecialOpNameNode) {
            var operator = ((SpecialOpNameNode) postDot).getOperator();
            return result.tryOperatorInfo(node.getLineInfo(), operator, info).toCallable();
        } else if (postDot instanceof IndexNode) {
            var index = (IndexNode) postDot;
            var variable = ((VariableNode) index.getVar()).getName();
            var attrType = result.tryAttrType(postDot, variable, info);
            var operator = index.getIndices()[0] instanceof SliceNode ? OpSpTypeNode.GET_SLICE : OpSpTypeNode.GET_ATTR;
            return attrType.tryOperatorReturnType(node, operator, info)[0];
        } else {
            throw CompilerInternalError.of("Unimplemented post-dot type", dot);
        }
    }

    private TypeObject optionalDotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("?");
        if (!(result instanceof OptionTypeObject)) {
            return normalDotReturnType(result, dot);
        }
        var postDot = dot.getPostDot();
        if (postDot instanceof VariableNode) {
            var retType = result.stripNull().tryAttrType(postDot, ((VariableNode) postDot).getName(), info);
            return TypeObject.optional(retType);
        } else if (postDot instanceof FunctionCallNode) {
            var caller = ((FunctionCallNode) postDot).getCaller();
            var attrType = result.stripNull().tryAttrType(postDot, ((VariableNode) caller).getName(), info);
            return TypeObject.optional(attrType.tryOperatorReturnType(node, OpSpTypeNode.CALL, info)[0]);
        } else {
            throw CompilerInternalError.of("Unimplemented post-dot type", dot);
        }
    }

    @NotNull
    private TypeObject nonNullReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        var hasNull = result instanceof OptionTypeObject;
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
        if (retCount == 0) {  // FIXME: Ensure return counts are correct
            bytes.add(Bytecode.POP_TOP.value);
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
        bytes.add(Bytecode.UNWRAP_OPTION.value);
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
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.strConstant())));  // TODO: Error type
        bytes.add(Bytecode.LOAD_CONST.value);
        var message = String.format("Value %s asserted non-null, was null", postDot);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        bytes.add(Bytecode.UNWRAP_OPTION.value);
        convertPostDot(start, bytes, postDot);
    }

    private void convertPostDot(int start, @NotNull List<Byte> bytes, @NotNull NameNode postDot) {
        if (postDot instanceof VariableNode) {
            bytes.add(Bytecode.LOAD_DOT.value);
            var name = LangConstant.of(((VariableNode) postDot).getName());
            bytes.addAll(Util.shortToBytes(info.constIndex(name)));
        } else if (postDot instanceof FunctionCallNode) {
            convertMethod(start, bytes, (FunctionCallNode) postDot);
        } else if (postDot instanceof SpecialOpNameNode) {
            var op = ((SpecialOpNameNode) postDot).getOperator();
            bytes.add(Bytecode.LOAD_OP.value);
            bytes.addAll(Util.shortToBytes((short) op.ordinal()));
        } else if (postDot instanceof IndexNode) {
            convertIndex(start, bytes, (IndexNode) postDot);
        } else {
            throw CompilerInternalError.of("This kind of post-dot not yet supported", postDot);
        }
    }

    private void convertMethod(int start, @NotNull List<Byte> bytes, @NotNull FunctionCallNode postDot) {
        var name = ((VariableNode) postDot.getCaller()).getName();
        for (var value : postDot.getParameters()) {  // TODO: Varargs, merge with FunctionCallNode
            bytes.addAll(TestConverter.bytes(start + bytes.size(), value.getArgument(), info, 1));
        }
        bytes.add(Bytecode.CALL_METHOD.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(name))));
        bytes.addAll(Util.shortToBytes((short) postDot.getParameters().length));
    }

    private void convertIndex(int start, @NotNull List<Byte> bytes, @NotNull IndexNode postDot) {
        var name = ((VariableNode) postDot.getVar()).getName();
        bytes.add(Bytecode.LOAD_DOT.value);
        var nameConst = LangConstant.of(name);
        bytes.addAll(Util.shortToBytes(info.constIndex(nameConst)));
        var indices = postDot.getIndices();
        if (indices[0] instanceof SliceNode) {
            assert indices.length == 1;
            var slice = (SliceNode) indices[0];
            bytes.addAll(new SliceConverter(info, slice).convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.GET_SLICE.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 1));
        } else {
            for (var value : indices) {  // TODO: Merge with IndexNode
                bytes.addAll(TestConverter.bytes(start + bytes.size(), value, info, 1));
            }
            if (indices.length == 1) {
                bytes.add(Bytecode.SUBSCRIPT.value);
            } else {
                bytes.add(Bytecode.CALL_OP.value);
                bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.GET_ATTR.ordinal()));
                bytes.addAll(Util.shortToBytes((short) indices.length));
            }
        }
    }
}
