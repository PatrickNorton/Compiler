package main.java.converter;

import main.java.parser.ElifStatementNode;
import main.java.parser.IfStatementNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IfConverter implements BaseConverter {
    private IfStatementNode node;
    private CompilerInfo info;

    public IfConverter(CompilerInfo info, IfStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        var bytes = new ArrayList<>(BaseConverter.bytes(start, node.getConditional(), info));
        boolean hasElse = !node.getElseStmt().isEmpty();
        addBody(bytes, start, node.getBody(), node.getElifs().length > 0 || hasElse);
        int elifsRemaining = node.getElifs().length - 1;
        for (var elif : node.getElifs()) {
            addElif(bytes, start, elif, hasElse || elifsRemaining > 0);
            elifsRemaining--;
        }
        if (hasElse) {
            addElse(bytes, start, node.getElseStmt());
        }
        return bytes;
    }

    private void addBody(@NotNull List<Byte> bytes, int start, StatementBodyNode body, boolean trailingJump) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP_FALSE.size(), body, info);
        // Jump index
        var jumpTarget = start + bytes.size() + Bytecode.JUMP_FALSE.size() + bodyBytes.size() + trailingJumpBytes(trailingJump);
        bytes.add(Bytecode.JUMP_FALSE.value);
        bytes.addAll(Util.intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }

    private void addElif(@NotNull List<Byte> bytes, int start, @NotNull ElifStatementNode elif, boolean trailingJump) {
        var cond = elif.getTest();
        var body = elif.getBody();
        bytes.add(Bytecode.JUMP.value);
        var jumpTarget = bytes.size();
        bytes.addAll(Util.zeroToBytes());  // Set jump target as temp value
        bytes.addAll(BaseConverter.bytes(start + Bytecode.JUMP.size() + bytes.size(), cond, info));
        addBody(bytes, start, body, trailingJump);
        // Set jump target
        var target = Util.intToBytes(start + bytes.size() + trailingJumpBytes(trailingJump));
        for (int i = 0; i < target.size(); i++) {
            bytes.set(jumpTarget + i, target.get(i));
        }
    }

    private void addElse(@NotNull List<Byte> bytes, int start, StatementBodyNode body) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP.size(), body, info);
        // Jump index
        var jumpTarget = start + bytes.size() + Bytecode.JUMP.size() + bodyBytes.size();
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }

    private int trailingJumpBytes(boolean trailingJump) {
        return trailingJump ? Bytecode.JUMP.size() : 0;
    }
}
