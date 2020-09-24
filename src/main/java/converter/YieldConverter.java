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
            boolean needsMakeOption;
            if (!fnReturn.isSuperclass(retType)) {
                if (OptionTypeObject.needsMakeOption(fnReturn, retType)
                        && OptionTypeObject.superWithOption(fnReturn, retType)) {
                    needsMakeOption = true;
                } else {
                    throw CompilerException.format(
                            "Type mismatch: in position %d, function expected" +
                                    " a superclass of type '%s' to be yielded, got type '%s'",
                            node, i, fnReturn.name(), retType.name()
                    );
                }
            } else {
                needsMakeOption = false;
            }
            bytes.addAll(converter.convert(start + bytes.size()));
            if (needsMakeOption) {
                bytes.add(Bytecode.MAKE_OPTION.value);
            }
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
        var needsOptions = checkReturnType(fnReturns, retType);
        bytes.addAll(converter.convert(start + bytes.size()));
        for (int i = 0; i < needsOptions.length; i++) {
            if (needsOptions[i]) {
                addSwap(bytes, needsOptions.length - i - 1);
                bytes.add(Bytecode.MAKE_OPTION.value);
                addSwap(bytes, needsOptions.length - i - 1);
            }
        }
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

    @NotNull
    private boolean[] checkReturnType(@NotNull TypeObject[] expected, @NotNull TypeObject[] gotten) {
        if (expected.length > gotten.length) {
            throw CompilerException.format(
                    "Mismatched types: function yields %d items, yield statement gave %d",
                    node, expected.length, gotten.length
            );
        }
        boolean[] result = new boolean[expected.length];
        for (int i = 0; i < expected.length; i++) {
            var fnReturn = expected[i];
            var retType = gotten[i];
            if (!expected[i].isSuperclass(gotten[i])) {
                if (OptionTypeObject.needsMakeOption(fnReturn, retType)
                        && OptionTypeObject.superWithOption(fnReturn, retType)) {
                    result[i] = true;
                } else {
                    throw CompilerException.format(
                            "Type mismatch: in position %d, function expected" +
                                    " a superclass of type '%s' to be yielded, got type '%s'",
                            node, i, expected[i].name(), gotten[i].name()
                    );
                }
            } else {
                result[i] = false;
            }
        }
        return result;
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

    private static void addSwap(List<Byte> bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2.value);
            default:
                bytes.add(Bytecode.SWAP_STACK.value);
                bytes.addAll(Util.shortZeroBytes());
                bytes.addAll(Util.shortToBytes((short) distFromTop));
        }
    }
}
