package main.java.converter;

import main.java.parser.ReturnStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        List<Byte> bytes = new ArrayList<>();
        int jumpPos;
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            jumpPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        } else {
            jumpPos = -1;
        }
        var retInfo = info.getFnReturns();
        if (retInfo.notInFunction()) {
            throw CompilerException.of("Cannot return from here", node);
        } else if (retInfo.isGenerator() && !node.getReturned().isEmpty()) {
            throw CompilerException.of("Return with arguments invalid in generator", node);
        }
        var fnReturns = retInfo.currentFnReturns();
        if (fnReturns.length == 0 || retInfo.isGenerator()) {  // Zero-returning functions are easy to deal with
            if (!node.getReturned().isEmpty()) {
                throw CompilerException.of(
                        "Non-empty 'return' statement invalid in function with no return types", node
                );
            } else {
                bytes.add(Bytecode.RETURN.value);
                bytes.addAll(Util.zeroToBytes());
            }
        } else {
            var retConverter = new ReturnListConverter(node.getReturned(), info, fnReturns, Bytecode.RETURN);
            bytes.addAll(retConverter.convert(start + bytes.size()));
        }
        if (jumpPos != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        }
        return bytes;
    }
}
