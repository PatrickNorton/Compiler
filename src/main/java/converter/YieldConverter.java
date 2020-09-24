package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.YieldStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class YieldConverter implements BaseConverter {
    private final YieldStatementNode node;
    private final CompilerInfo info;

    public YieldConverter(CompilerInfo info, YieldStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        int jumpPos;
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            jumpPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        } else {
            jumpPos = -1;
        }
        if (node.getYielded().size() > 1) {
            throw CompilerTodoError.of("Cannot yield more than one value yet", node);
        }
        if (node.getYielded().isEmpty()) {
            throw CompilerException.of("Empty yield statements are illegal", node);
        } if (node.isFrom()) {
            convertFrom(start, bytes);
        } else if (info.getFnReturns().currentFnReturns().length > 1 && node.getYielded().size() > 1) {
            convertSingle(start, bytes);
        } else {
            convertNormal(start, bytes);
        }
        if (jumpPos != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        }
        return bytes;
    }

    private void convertNormal(int start, List<Byte> bytes) {
        var retInfo = info.getFnReturns();
        if (!retInfo.isGenerator()) {
            throw noGeneratorError();
        }
        var fnReturns = retInfo.currentFnReturns();
        checkVarargs();
        if (fnReturns.length != node.getYielded().size()) {
            var tooMany = node.getYielded().size() > fnReturns.length;
            throw CompilerException.format(
                    "%s values in yield statement: expected %d, got %d",
                    node, tooMany ? "Too many" : "Not enough", fnReturns.length, node.getYielded().size()
            );
        }
        for (int i = 0; i < fnReturns.length; i++) {
            var yielded = node.getYielded().get(i);
            var fnReturn = fnReturns[i];
            var converter = TestConverter.of(info, yielded, 1);
            var retType = converter.returnType()[0];
            if (!fnReturn.isSuperclass(retType)) {
                throw CompilerException.format(
                        "Type mismatch: in position %d, function expected" +
                                " a superclass of type '%s' to be yielded, got type '%s'",
                        node, i, fnReturn.name(), retType.name()
                );
            }
            bytes.addAll(converter.convert(start + bytes.size()));
        }
        bytes.add(Bytecode.YIELD.value);
        bytes.addAll(Util.shortToBytes((short) fnReturns.length));
    }

    private void convertSingle(int start, List<Byte> bytes) {
        var retInfo = info.getFnReturns();
        if (!retInfo.isGenerator()) {
            throw noGeneratorError();
        }
        var fnReturns = retInfo.currentFnReturns();
        var retCount = fnReturns.length;
        assert retCount > 1 && node.getYielded().size() == 1;
        var converter = TestConverter.of(info, node.getYielded().get(0), retCount);
        var retType = converter.returnType();
        checkReturnType(fnReturns, retType);
        bytes.addAll(converter.convert(start + bytes.size()));
        bytes.add(Bytecode.YIELD.value);
        bytes.addAll(Util.shortToBytes((short) retCount));
    }

    private void convertFrom(int start, List<Byte> bytes) {
        var retInfo = info.getFnReturns();
        if (!retInfo.isGenerator()) {
            throw noGeneratorError();
        }
        var converter = TestConverter.of(info, node.getYielded().get(0), retInfo.currentFnReturns().length);
        var rawRet = converter.returnType()[0].tryOperatorReturnType(node, OpSpTypeNode.ITER, info)[0];
        var retTypes = Builtins.deIterable(rawRet);
        checkReturnType(retInfo.currentFnReturns(), retTypes);
        ForConverter.addIter(info, start, bytes, converter);
        bytes.add(Bytecode.FOR_ITER.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(Util.shortToBytes((short) 1));
        bytes.add(Bytecode.YIELD.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(jumpPos - 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
    }

    private void checkReturnType(@NotNull TypeObject[] expected, @NotNull TypeObject[] gotten) {
        if (expected.length > gotten.length) {
            throw CompilerException.format(
                    "Mismatched types: function yields %d items, yield statement gave %d",
                    node, expected.length, gotten.length
            );
        }
        for (int i = 0; i < gotten.length; i++) {
            if (!expected[i].isSuperclass(gotten[i])) {
                throw CompilerException.format(
                        "Type mismatch: in position %d, function expected" +
                                " a superclass of type '%s' to be yielded, got type '%s'",
                        node, i, expected[i].name(), gotten[i].name()
                );
            }
        }
    }

    @NotNull
    private CompilerException noGeneratorError() {
        return CompilerException.of("'yield' is only valid in a generator", node);
    }

    private void checkVarargs() {
        for (var pair : node.getYielded()) {
            if (!pair.getValue().isEmpty()) {
                throw CompilerTodoError.of("Cannot use varargs with yield yet", pair.getKey());
            }
        }
    }
}
