package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.YieldStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class YieldConverter implements BaseConverter {
    private final YieldStatementNode node;
    private final CompilerInfo info;

    public YieldConverter(CompilerInfo info, YieldStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public @NotNull List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        var bytes = new BytecodeList();
        var jumpLbl = IfConverter.addJump(bytes, node.getCond(), info);
        if (node.getYielded().isEmpty()) {
            throw CompilerException.of("Empty yield statements are illegal", node);
        } else if (node.isFrom()) {
            convertFrom(bytes);
        } else {
            var retInfo = info.getFnReturns();
            if (!retInfo.isGenerator()) {
                throw noGeneratorError();
            }
            var fnReturns = retInfo.currentFnReturns();
            var converter = new ReturnListConverter(node.getYielded(), info, fnReturns, Bytecode.YIELD);
            bytes.addAll(converter.convert());
        }
        if (jumpLbl != null) {
            bytes.addLabel(jumpLbl);
        }
        return bytes;
    }

    private void convertFrom(BytecodeList bytes) {
        assert node.isFrom();
        var retInfo = info.getFnReturns();
        if (!retInfo.isGenerator()) {
            throw noGeneratorError();
        }
        var converter = TestConverter.of(info, node.getYielded().get(0), retInfo.currentFnReturns().length);
        var rawRet = converter.returnType()[0].tryOperatorReturnType(node, OpSpTypeNode.ITER, info)[0];
        var retTypes = Builtins.deIterable(rawRet);
        checkReturnType(retInfo.currentFnReturns(), retTypes);
        ForConverter.addIter(info, bytes, converter);
        var topLabel = info.newJumpLabel();
        bytes.addLabel(topLabel);
        var label = info.newJumpLabel();
        bytes.add(Bytecode.FOR_ITER, label, 1);
        bytes.add(Bytecode.YIELD, 1);
        bytes.add(Bytecode.JUMP, topLabel);
        bytes.addLabel(label);
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
                if (!OptionTypeObject.needsAndSuper(fnReturn, retType)) {
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
