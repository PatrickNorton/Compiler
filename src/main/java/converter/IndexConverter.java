package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IndexConverter implements TestConverter {
    private final IndexNode node;
    private final CompilerInfo info;
    private final int retCount;

    public IndexConverter(CompilerInfo info, IndexNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        if (node.getIndices().length > 1) {
            // No constant-foldable type has more than 1 index value yet
            return Optional.empty();
        }
        var preDotConst = TestConverter.constantReturn(node.getVar(), info, 1);
        if (preDotConst.isEmpty()) {
            return Optional.empty();
        }
        var preDot = preDotConst.orElseThrow();
        if (preDot instanceof StringConstant) {
            return stringConstant(((StringConstant) preDot).getValue());
        } else if (preDot instanceof RangeConstant) {
            return rangeConstant((RangeConstant) preDot);
        } else if (preDot instanceof BytesConstant) {
            return bytesConstant(((BytesConstant) preDot).getValue());
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var type = TypeObject.of(info, node);
        if (type != null) {
            return new TypeObject[]{Builtins.TYPE.generify(type)};
        }
        var operator = isSlice() ? OpSpTypeNode.GET_SLICE : OpSpTypeNode.GET_ATTR;
        return TestConverter.returnType(node.getVar(), info, 1)[0].tryOperatorReturnType(node, operator, info);
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getVar(), info, 1));
        if (isSlice()) {
            checkSliceType();
            bytes.addAll(new SliceConverter(info, (SliceNode) node.getIndices()[0]).convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.GET_SLICE.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 1));
        } else {
            for (var index : node.getIndices()) {
                bytes.addAll(TestConverter.bytes(start, index, info, 1));
            }
            bytes.add(Bytecode.LOAD_SUBSCRIPT.value);
            bytes.addAll(Util.shortToBytes((short) node.getIndices().length));
            if (retCount == 0) {
                bytes.add(Bytecode.POP_TOP.value);
            }
        }
        return bytes;
    }

    public List<Byte> convertIterSlice(int start) {
        assert isSlice();
        var converter = TestConverter.of(info, node.getVar(), 1);
        var ret = converter.returnType()[0];
        var hasIter = ret.operatorInfo(OpSpTypeNode.ITER_SLICE, info).isPresent();
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getVar(), info, 1));
        checkSliceType();
        bytes.addAll(new SliceConverter(info, (SliceNode) node.getIndices()[0]).convert(start + bytes.size()));
        bytes.add(Bytecode.CALL_OP.value);
        var ordinal = (hasIter ? OpSpTypeNode.ITER_SLICE : OpSpTypeNode.GET_SLICE).ordinal();
        bytes.addAll(Util.shortToBytes((short) ordinal));
        bytes.addAll(Util.shortToBytes((short) 1));
        return bytes;
    }

    private void checkSliceType() {
        var retType = TestConverter.returnType(node.getVar(), info, 1)[0];
        var fnInfo = retType.tryOperatorInfo(node.getLineInfo(), OpSpTypeNode.GET_SLICE, info);
        if (!fnInfo.matches(new Argument("", Builtins.SLICE))) {
            throw CompilerException.format(
                    "Type '%s' has an operator [:] that does not take a slice as its argument",
                    node, retType.name()
            );
        }
    }

    private Optional<BigInteger> indexConstant() {
        assert node.getIndices().length == 1;
        return TestConverter.constantReturn(node.getIndices()[0], info, 1).flatMap(IntArithmetic::convertConst);
    }

    private Optional<Integer> intIndexConstant() {
        assert node.getIndices().length == 1;
        return TestConverter.constantReturn(node.getIndices()[0], info, 1).flatMap(IntArithmetic::convertToInt);
    }

    private Optional<LangConstant> stringConstant(String value) {
        var maybeIndex = intIndexConstant();
        if (maybeIndex.isPresent()) {
            var index = maybeIndex.orElseThrow();
            var result = value.codePoints().skip(index).findFirst();
            if (result.isPresent()) {
                return Optional.of(new CharConstant(result.orElseThrow()));
            } else {
                return Optional.empty();

            }
        }
        return Optional.empty();
    }

    private Optional<LangConstant> rangeConstant(RangeConstant value) {
        var maybeIndex = indexConstant();
        if (maybeIndex.isPresent()) {
            var index = maybeIndex.orElseThrow();
            if (value.getStart().isPresent()) {
                var start = value.getStart().orElseThrow();
                var step = value.getStep().orElse(BigInteger.ONE);
                var result = start.add(index.multiply(step));
                if (value.getStop().isEmpty() || inRange(result, value.getStop().orElseThrow(), step)) {
                    return Optional.of(LangConstant.of(result));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean inRange(BigInteger value, BigInteger stop, BigInteger step) {
        if (step.signum() > 0) {
            return value.compareTo(stop) < 0;
        } else {
            return value.compareTo(stop) > 0;
        }
    }

    private Optional<LangConstant> bytesConstant(List<Byte> value) {
        var maybeIndex = intIndexConstant();
        if (maybeIndex.isPresent()) {
            var index = maybeIndex.orElseThrow();
            if (index < value.size()) {
                return Optional.of(LangConstant.of(value.get(index)));
            }
        }
        return Optional.empty();
    }

    public boolean isSlice() {
        return isSlice(node.getIndices());
    }

    public static boolean isSlice(TestNode[] indices) {
        return indices.length == 1 && indices[0] instanceof SliceNode;
    }

    static List<Byte> convertDuplicate(int start, TestConverter converter, TestNode[] indices, CompilerInfo info) {
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        for (var param : indices) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), param, info, 1));
        }
        if (indices.length == 1) {
            bytes.add(Bytecode.DUP_TOP_2.value);
        } else {
            bytes.add(Bytecode.DUP_TOP_N.value);
            bytes.addAll(Util.shortToBytes((short) (indices.length + 1)));
        }
        bytes.add(Bytecode.LOAD_SUBSCRIPT.value);
        return bytes;
    }
}
