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
        int jumpPos = IfConverter.addJump(start, bytes, node.getCond(), info);
        if (node.getYielded().isEmpty()) {
            throw CompilerException.of("Empty yield statements are illegal", node);
        } else if (node.isFrom()) {
            convertFrom(start, bytes);
        } else {
            var retInfo = info.getFnReturns();
            if (!retInfo.isGenerator()) {
                throw noGeneratorError();
            }
            var fnReturns = retInfo.currentFnReturns();
            var converter = new ReturnListConverter(node.getYielded(), info, fnReturns, Bytecode.YIELD);
            bytes.addAll(converter.convert(start + bytes.size()));
        }
        if (jumpPos != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        }
        return bytes;
    }

    private void convertFrom(int start, List<Byte> bytes) {
        assert node.isFrom();
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
        for (int i = 0; i < expected.length; i++) {
            var fnReturn = expected[i];
            var retType = gotten[i];
            if (!expected[i].isSuperclass(gotten[i])) {
                if (!OptionTypeObject.needsMakeOption(fnReturn, retType)
                        || !OptionTypeObject.superWithOption(fnReturn, retType)) {
                    throw CompilerException.format(
                            "Type mismatch: in position %d, function expected" +
                                    " a superclass of type '%s' to be yielded, got type '%s'",
                            node, i, expected[i].name(), gotten[i].name()
                    );
                }
            }
        }
    }

    @NotNull
    private CompilerException noGeneratorError() {
        return CompilerException.of("'yield' is only valid in a generator", node);
    }
}
