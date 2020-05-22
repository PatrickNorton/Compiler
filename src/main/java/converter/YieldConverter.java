package main.java.converter;

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
            throw new UnsupportedOperationException("Cannot yield more than one value yet");
        }
        var retInfo = info.getFnReturns();
        if (!retInfo.isGenerator()) {
            throw CompilerException.of("'yield' is only valid in a generator", node);
        }
        var converter = TestConverter.of(info, node.getYielded().get(0), retInfo.currentFnReturns().length);
        var retType = converter.returnType();
        checkReturnType(retInfo.currentFnReturns(), retType);
        bytes.addAll(converter.convert(start + bytes.size()));
        bytes.add(Bytecode.YIELD.value);
        bytes.addAll(Util.shortToBytes((short) retInfo.currentFnReturns().length));
        if (jumpPos != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        }
        return bytes;
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
}
