package main.java.converter;

import main.java.parser.OperatorNode;
import main.java.parser.WhileStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WhileConverter extends LoopConverter {
    private final WhileStatementNode node;

    public WhileConverter(CompilerInfo info, WhileStatementNode node) {
        super(info);
        this.node = node;
    }

    @NotNull
    @Override
    protected List<Byte> trueConvert(int start) {
        List<Byte> bytes = new ArrayList<>();
        boolean hasAs = !node.getAs().isEmpty();
        info.setContinuePoint(start + bytes.size());
        if (!hasAs) {
            var cond = TestConverter.bytes(start + bytes.size(), node.getCond(), info, 1);
            bytes.addAll(cond);
        } else {
            convertCondWithAs(bytes, start);
        }
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        if (hasAs) {
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(info.varIndex(node.getAs().getName())));
        }
        var body = BaseConverter.bytes(start + bytes.size(), node.getBody(), info);
        bytes.addAll(body);
        bytes.add(Bytecode.JUMP.value);
        info.addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        if (!node.getNobreak().isEmpty()) {
            addNobreak(bytes, start, jumpLoc);
        } else if (hasAs) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            bytes.add(Bytecode.POP_TOP.value);
        } else {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        }
        return bytes;
    }

    private void convertCondWithAs(List<Byte> bytes, int start) {
        var condition = node.getCond();
        if (!(condition instanceof OperatorNode)) {
            throw CompilerException.of(
                    "Cannot use 'as' here: condition must be 'instanceof' or 'is not null'", condition
            );
        }
        var pair = OperatorConverter.convertWithAs(start + bytes.size(), (OperatorNode) condition, info, 1);
        info.addStackFrame();
        info.addVariable(node.getAs().getName(), pair.getValue());
        bytes.addAll(pair.getKey());
    }

    private void addNobreak(@NotNull List<Byte> bytes, int start, int jumpLoc) {
        var nobreak = BaseConverter.bytes(start + bytes.size(), node.getNobreak(), info);
        bytes.addAll(nobreak);
        if (!node.getAs().isEmpty()) {
            int jumpPos = start + bytes.size() + Bytecode.JUMP.size() + Bytecode.POP_TOP.size();
            bytes.add(Bytecode.JUMP.value);
            bytes.addAll(Util.intToBytes(jumpPos));
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            bytes.add(Bytecode.POP_TOP.value);
        }
    }
}
