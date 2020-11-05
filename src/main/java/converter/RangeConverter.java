package main.java.converter;

import main.java.parser.RangeLiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class RangeConverter implements TestConverter {
    private final RangeLiteralNode node;
    private final CompilerInfo info;
    private final int retCount;

    public RangeConverter(CompilerInfo info, RangeLiteralNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        BigInteger start;
        if (node.getStart().isEmpty()) {
            start = null;
        } else {
            var ret = retVal(node.getStart());
            if (ret.isEmpty()) {
                return Optional.empty();
            }
            start = ret.orElseThrow();
        }
        BigInteger end;
        if (node.getEnd().isEmpty()) {
            end = null;
        } else {
            var ret = retVal(node.getEnd());
            if (ret.isEmpty()) {
                return Optional.empty();
            }
            end = ret.orElseThrow();
        }
        BigInteger step;
        if (node.getStep().isEmpty()) {
            step = null;
        } else {
            var ret = retVal(node.getStep());
            if (ret.isEmpty()) {
                return Optional.empty();
            }
            step = ret.orElseThrow();
        }
        return Optional.of(new RangeConstant(start, end, step));
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.RANGE};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Range literal creation has unused result", node);
            return Collections.emptyList();
        }
        var constVal = constantReturn();
        if (constVal.isPresent()) {
            var constant = constVal.orElseThrow();
            List<Byte> bytes = new ArrayList<>(Bytecode.LOAD_CONST.size());
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(constant)));
            return bytes;
        }
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        var constant = info.constIndex(Builtins.rangeConstant());
        bytes.addAll(Util.shortToBytes(constant));
        convertPortion(start, bytes, node.getStart(), 0);
        convertPortion(start, bytes, node.getEnd(), 0);
        convertPortion(start, bytes, node.getStep(), 1);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 3));
        return bytes;
    }

    private void convertPortion(int start, List<Byte> bytes, @NotNull TestNode node, int defaultVal) {
        if (!node.isEmpty()) {
            var converter = TestConverter.of(info, node, 1);
            if (!Builtins.INT.isSuperclass(converter.returnType()[0])) {
                throw CompilerException.format(
                        "TypeError: Type %s does not match required type %s",
                        node, converter.returnType()[0].name(), Builtins.INT.name()
                );
            }
            bytes.addAll(converter.convert(start));
        } else {
            var constIndex = info.constIndex(LangConstant.of(defaultVal));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(constIndex));
        }
    }

    private Optional<BigInteger> retVal(TestNode value) {
        return TestConverter.constantReturn(value, info, 1).flatMap(RangeConverter::fromConstant);
    }

    private static Optional<BigInteger> fromConstant(LangConstant value) {
        if (value instanceof BigintConstant) {
            return Optional.of(((BigintConstant) value).getValue());
        } else if (value instanceof IntConstant) {
            return Optional.of(BigInteger.valueOf(((IntConstant) value).getValue()));
        } else {
            return Optional.empty();
        }
    }
}
