package main.java.converter;

import main.java.parser.DottedVar;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IndexNode;
import main.java.parser.NameNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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
        var last = node.getLast();
        var postDots = node.getPostDots();
        if (!(last.getPostDot() instanceof VariableNode)) {
            throw CompilerInternalError.of(
                    "DotConverter.exceptLast does not work where the last dot is not plain", last
            );
        }
        if (postDots.length == 1) {
            var converter = TestConverter.of(info, node.getPreDot(), retCount);
            var postDot = (VariableNode) last.getPostDot();
            return Pair.of(converter, postDot.getName());
        } else {
            var newPostDots = Arrays.copyOf(postDots, postDots.length - 1);
            var newNode = new DottedVariableNode(node.getPreDot(), newPostDots);
            var converter = TestConverter.of(info, newNode, retCount);
            var postDot = (VariableNode) last.getPostDot();
            return Pair.of(converter, postDot.getName());
        }
    }

    public static Pair<TestConverter, TestNode[]> exceptLastIndex(
            CompilerInfo info, @NotNull DottedVariableNode node, int retCount
    ) {
        var last = node.getLast();
        var postDots = node.getPostDots();
        if (!(last.getPostDot() instanceof IndexNode)) {
            throw CompilerInternalError.of(
                    "DotConverter.exceptLastIndex does not work where the last dot is not an index", last
            );
        }
        var newPostDots = Arrays.copyOf(postDots, postDots.length);
        var postDot = (IndexNode) last.getPostDot();
        var dot = new DottedVar(last.getLineInfo(), last.getDotPrefix(), (NameNode) postDot.getVar());
        newPostDots[newPostDots.length - 1] = dot;
        var newNode = new DottedVariableNode(node.getPreDot(), newPostDots);
        var converter = TestConverter.of(info, newNode, retCount);
        return Pair.of(converter, postDot.getIndices());
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var result = TestConverter.returnType(node.getPreDot(), info, 1);
        for (int i = 0; i < node.getPostDots().length; i++) {
            var dot = node.getPostDots()[i];
            if (result.length == 0) {  // Check that the previous one returned a value
                throw CompilerException.of(
                        "Dot does not return a value, expected at least 1",
                        i == 0 ? node.getPreDot() : node.getPostDots()[i - 1]
                );
            }
            result = dotReturnType(result[0], dot);
        }
        if (retCount > result.length) {
            throw CompilerException.format(
                    "Expected at least %d returns, but only got %d",
                    node.getLast(), retCount, result.length
            );
        }
        return Arrays.copyOf(result, retCount);
    }

    @NotNull
    private TypeObject[] dotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        switch (dot.getDotPrefix()) {
            case "":
                return normalDotReturnType(result, dot);
            case "?":
                return optionalDotReturnType(result, dot);
            case "!!":
                return nonNullReturnType(result, dot);
            default:
                throw CompilerInternalError.of("Unknown type of dot " + dot.getDotPrefix(), dot);
        }
    }

    @NotNull
    private TypeObject[] normalDotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().isEmpty() || !result.isSuperclass(Builtins.nullType());
        var postDot = dot.getPostDot();
        if (postDot instanceof VariableNode) {
            return new TypeObject[]{result.tryAttrType(postDot, ((VariableNode) postDot).getName(), info)};
        } else if (postDot instanceof FunctionCallNode) {
            var caller = ((FunctionCallNode) postDot).getCaller();
            TypeObject attrType;
            if (caller instanceof SpecialOpNameNode) {
                var op = ((SpecialOpNameNode) caller).getOperator();
                attrType = result.tryOperatorInfo(postDot, op, info).toCallable();
            } else if (caller instanceof VariableNode) {
                attrType = result.tryAttrType(postDot, ((VariableNode) caller).getName(), info);
            } else if (caller instanceof NameNode) {
                var dottedVar = new DottedVar(dot.getLineInfo(), dot.getDotPrefix(), (NameNode) caller);
                var preCallType = normalDotReturnType(result, dottedVar)[0];
                return preCallType.tryOperatorReturnType(node, OpSpTypeNode.CALL, info);
            } else {
                throw CompilerInternalError.of("Invalid function-call prefix", postDot);
            }
            var opInfo = attrType.tryOperatorInfo(postDot, OpSpTypeNode.CALL, info);
            var retTypes = opInfo.getReturns();
            if (retTypes.length == 0) {
                return new TypeObject[0];
            }
            var params = ((FunctionCallNode) postDot).getParameters();
            return FunctionCallConverter.generifyReturns(opInfo, info, params, postDot, -1);
        } else if (postDot instanceof SpecialOpNameNode) {
            var operator = ((SpecialOpNameNode) postDot).getOperator();
            return new TypeObject[]{result.tryOperatorInfo(node.getLineInfo(), operator, info).toCallable()};
        } else if (postDot instanceof IndexNode) {
            var index = (IndexNode) postDot;
            var variable = new DottedVar(dot.getLineInfo(), dot.getDotPrefix(),  (NameNode) index.getVar());
            var attrType = normalDotReturnType(result, variable)[0];
            var operator = IndexConverter.isSlice(index.getIndices())
                    ? OpSpTypeNode.GET_SLICE : OpSpTypeNode.GET_ATTR;
            return new TypeObject[]{attrType.tryOperatorReturnType(node, operator, info)[0]};
        } else {
            throw CompilerInternalError.of("Unimplemented post-dot type", dot);
        }
    }

    private TypeObject[] optionalDotReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        assert dot.getDotPrefix().equals("?");
        if (!(result instanceof OptionTypeObject)) {
            return normalDotReturnType(result, dot);
        }
        var postDot = dot.getPostDot();
        if (postDot instanceof VariableNode) {
            var retType = result.stripNull().tryAttrType(postDot, ((VariableNode) postDot).getName(), info);
            return new TypeObject[]{TypeObject.optional(retType)};
        } else if (postDot instanceof FunctionCallNode) {
            var caller = ((FunctionCallNode) postDot).getCaller();
            TypeObject attrType;
            if (caller instanceof SpecialOpNameNode) {
                var op = ((SpecialOpNameNode) caller).getOperator();
                attrType = result.stripNull().tryOperatorInfo(postDot, op, info).toCallable();
            } else {
                attrType = result.stripNull().tryAttrType(postDot, ((VariableNode) caller).getName(), info);
            }
            var endType = TypeObject.optional(attrType.tryOperatorReturnType(node, OpSpTypeNode.CALL, info)[0]);
            return new TypeObject[]{endType};
        } else {
            throw CompilerInternalError.of("Unimplemented post-dot type", dot);
        }
    }

    @NotNull
    private TypeObject[] nonNullReturnType(@NotNull TypeObject result, @NotNull DottedVar dot) {
        var hasNull = result instanceof OptionTypeObject;
        var bangType = hasNull ? result.stripNull() : result;
        return normalDotReturnType(bangType, dot);
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        var preConverter = TestConverter.of(info, node.getPreDot(), 1);
        var bytes = new BytecodeList(preConverter.convert());
        TypeObject[] previous = preConverter.returnType();
        for (var dot : node.getPostDots()) {
            if (previous.length == 0) {
                throw CompilerException.of("Expected at least 1 return, got 0", dot.getPostDot());
            }
            var prev = previous[0];
            switch (dot.getDotPrefix()) {
                case "":
                    previous = convertNormal(prev, bytes, dot);
                    break;
                case "?":
                    previous = convertNullDot(prev, bytes, dot);
                    break;
                case "!!":
                    previous = convertNotNullDot(prev, bytes, dot);
                    break;
                default:
                    throw CompilerInternalError.format("Unknown value for dot prefix: '%s'", dot, dot.getDotPrefix());
            }
        }
        if (previous.length < retCount) {
            throw CompilerException.format(
                    "Cannot convert: %d returns is less than the required %d", node, previous.length, retCount
            );
        }
        for (int i = retCount; i < previous.length; i++) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    private TypeObject[] convertNormal(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull DottedVar dot
    ) {
        assert dot.getDotPrefix().isEmpty();
        var postDot = dot.getPostDot();
        return convertPostDot(previous, bytes, postDot);
    }

    private TypeObject[] convertNullDot(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull DottedVar dot
    ) {
        assert dot.getDotPrefix().equals("?");
        var postDot = dot.getPostDot();
        if (!(previous instanceof OptionTypeObject)) {
            CompilerWarning.warnf(
                    "Using ?. operator on non-optional type %s", WarningType.TRIVIAL_VALUE, info, dot, previous.name()
            );
            if (previous.sameBaseType(Builtins.nullType())) {
                bytes.add(Bytecode.POP_TOP);
                bytes.add(Bytecode.LOAD_NULL);
                return new TypeObject[]{Builtins.nullType()};
            } else {
                return convertPostDot(previous.stripNull(), bytes, postDot);
            }
        } else {
            bytes.add(Bytecode.DUP_TOP);
            var jumpLbl = info.newJumpLabel();
            bytes.add(Bytecode.JUMP_NULL, jumpLbl);
            bytes.add(Bytecode.UNWRAP_OPTION);
            var result = convertPostDot(previous.stripNull(), bytes, postDot);
            bytes.add(Bytecode.MAKE_OPTION);
            bytes.addLabel(jumpLbl);
            return new TypeObject[]{TypeObject.optional(result[0])};
        }
    }

    private TypeObject[] convertNotNullDot(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull DottedVar dot
    ) {
        assert dot.getDotPrefix().equals("!!");
        if (previous.sameBaseType(Builtins.nullType())) {
            throw CompilerException.format("Cannot use !! operator on null type", dot);
        } else if (!(previous instanceof OptionTypeObject)) {
            throw CompilerException.format("Cannot use !! operator on non-optional type", dot);
        }
        var postDot = dot.getPostDot();
        bytes.addAll(NullOpConverter.unwrapOption(info, postDot.toString()));
        return convertPostDot(previous.stripNull(), bytes, postDot);
    }

    private TypeObject[] convertPostDot(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull NameNode postDot
    ) {
        if (postDot instanceof VariableNode) {
            var strName = ((VariableNode) postDot).getName();
            var type = previous.tryAttrType(postDot, strName, info);
            bytes.add(Bytecode.LOAD_DOT, info.constIndex(LangConstant.of(strName)));
            return new TypeObject[] {type};
        } else if (postDot instanceof FunctionCallNode) {
            return convertMethod(previous, bytes, (FunctionCallNode) postDot);
        } else if (postDot instanceof SpecialOpNameNode) {
            var op = ((SpecialOpNameNode) postDot).getOperator();
            bytes.add(Bytecode.LOAD_OP, op.ordinal());
            return new TypeObject[]{previous.tryOperatorInfo(postDot, op, info).toCallable()};
        } else if (postDot instanceof IndexNode) {
            return convertIndex(previous, bytes, (IndexNode) postDot);
        } else {
            throw CompilerInternalError.of("This kind of post-dot not yet supported", postDot);
        }
    }

    private TypeObject[] convertMethod(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull FunctionCallNode postDot
    ) {
        if (postDot.getCaller() instanceof SpecialOpNameNode) {
            return convertOperator(previous, bytes, postDot);
        } else if (postDot.getCaller() instanceof VariableNode) {
            return convertNameMethod(previous, bytes, postDot);
        } else if (postDot.getCaller() instanceof NameNode) {
            return convertOtherMethod(previous, bytes, postDot);
        } else {
            throw CompilerInternalError.of("Invalid function-call prefix", postDot);
        }
    }

    private TypeObject[] convertOperator(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull FunctionCallNode postDot
    ) {
        var op = ((SpecialOpNameNode) postDot.getCaller()).getOperator();
        var type = previous.tryOperatorInfo(postDot, op, info);
        var pair = FunctionCallConverter.convertArgs(
                info, postDot, type.toCallable(), postDot.getParameters()
        );
        bytes.addAll(pair.getKey());
        bytes.add(Bytecode.CALL_OP, op.ordinal(), pair.getValue().shortValue());
        return type.getReturns();
    }

    private TypeObject[] convertNameMethod(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull FunctionCallNode postDot
    ) {
        var name = ((VariableNode) postDot.getCaller()).getName();
        var type = previous.tryAttrType(postDot, name, info);
        var pair = FunctionCallConverter.convertArgs(
                info, postDot, type, postDot.getParameters()
        );
        bytes.addAll(pair.getKey());
        bytes.add(Bytecode.CALL_METHOD, info.constIndex(LangConstant.of(name)), pair.getValue());
        return type.tryOperatorReturnType(postDot, OpSpTypeNode.CALL, info);
    }

    private TypeObject[] convertOtherMethod(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull FunctionCallNode postDot
    ) {
        var caller = (NameNode) postDot.getCaller();
        var type = convertPostDot(previous, bytes, caller)[0];
        var pair = FunctionCallConverter.convertArgs(
                info, postDot, type, postDot.getParameters()
        );
        bytes.addAll(pair.getKey());
        bytes.add(Bytecode.CALL_TOS, pair.getValue());
        return type.tryOperatorReturnType(postDot, OpSpTypeNode.CALL, info);
    }

    private TypeObject[] convertIndex(
            TypeObject previous, @NotNull BytecodeList bytes, @NotNull IndexNode postDot
    ) {
        var preIndex = (NameNode) postDot.getVar();
        var type = convertPostDot(previous, bytes, preIndex)[0];
        var indices = postDot.getIndices();
        if (IndexConverter.isSlice(indices)) {
            assert indices.length == 1;
            var result = type.tryOperatorInfo(node, OpSpTypeNode.GET_SLICE, info);
            var slice = (SliceNode) indices[0];
            bytes.addAll(new SliceConverter(info, slice).convert());
            bytes.add(Bytecode.CALL_OP, OpSpTypeNode.GET_SLICE.ordinal(), 1);
            return result.getReturns();
        } else {
            var result = type.tryOperatorInfo(node, OpSpTypeNode.GET_ATTR, info);
            bytes.addAll(IndexConverter.convertIndices(info, indices));
            return result.getReturns();
        }
    }
}
