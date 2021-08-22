package main.java.converter;

import main.java.parser.ReturnStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReturnConverter implements BaseConverter {
    private final ReturnStatementNode node;
    private final CompilerInfo info;

    public ReturnConverter(CompilerInfo info, ReturnStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        var bytes = new BytecodeList();
        var jumpPos = IfConverter.addJump(bytes, node.getCond(), info);
        var retInfo = info.getFnReturns();
        if (retInfo.notInFunction()) {
            throw CompilerException.of("Cannot return from here", node);
        } else if (retInfo.isGenerator() && !node.getReturned().isEmpty()) {
            throw CompilerException.of("Return with arguments invalid in generator", node);
        }
        var fnReturns = retInfo.currentFnReturns();
        if (fnReturns.length == 0 || retInfo.isGenerator()) {  // Zero-returning functions are easy to deal with
            convertEmpty(bytes);
        } else {
            var retConverter = new ReturnListConverter(node.getReturned(), info, fnReturns, Bytecode.RETURN);
            bytes.addAll(retConverter.convert());
        }
        if (jumpPos != null) {
            bytes.addLabel(jumpPos);
        }
        return bytes;
    }

    @Override
    @NotNull
    public Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        var divergingInfo = node.getCond().isEmpty()
                ? new DivergingInfo().knownReturn() : new DivergingInfo().possibleReturn();
        return Pair.of(convert(), divergingInfo);
    }

    private void convertEmpty(BytecodeList bytes) {
        if (!node.getReturned().isEmpty()) {
            throw CompilerException.of(
                    "Non-empty 'return' statement invalid in function with no return types", node
            );
        } else {
            bytes.add(Bytecode.RETURN, 0);
        }
    }
}
