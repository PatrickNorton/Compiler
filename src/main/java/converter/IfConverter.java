package main.java.converter;

import main.java.parser.ElifStatementNode;
import main.java.parser.IfStatementNode;
import main.java.parser.OperatorNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
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

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes;
        boolean hasAs = !node.getAs().isEmpty();
        if (hasAs) {
            bytes = addAs(start, node.getConditional(), node.getAs());
        } else {
            bytes = new ArrayList<>(TestConverter.bytes(start, node.getConditional(), info, 1));
        }
        boolean hasElse = !node.getElseStmt().isEmpty();
        boolean trailingJump = node.getElifs().length > 0 || hasElse;
        if (hasAs) {
            var asName = node.getAs().getName();
            // 'as' always needs a jump to ensure the correct popping occurs
            addBodyWithAs(bytes, start, node.getBody(), true, asName);
            info.removeStackFrame();
        } else {
            addBody(bytes, start, node.getBody(), trailingJump);
        }
        int elifsRemaining = node.getElifs().length - 1;
        boolean popAs = hasAs;
        for (var elif : node.getElifs()) {
            addElif(bytes, start, elif, hasElse || elifsRemaining > 0 || !elif.getAs().isEmpty(), popAs);
            popAs = !elif.getAs().isEmpty();
            elifsRemaining--;
        }
        if (hasElse) {
            addElse(bytes, start, node.getElseStmt(), popAs);
        } else if (popAs) {
            var jumpPos = start + bytes.size() + Bytecode.JUMP.size() + Bytecode.POP_TOP.size();
            bytes.add(Bytecode.JUMP.value);
            bytes.addAll(Util.intToBytes(jumpPos));
            bytes.add(Bytecode.POP_TOP.value);
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

    private void addBodyWithAs(@NotNull List<Byte> bytes, int start, StatementBodyNode body,
                               boolean trailingJump, String asName) {
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jumpInsert = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(asName)));
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), body, info));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size() + trailingJumpBytes(trailingJump)), jumpInsert);
    }

    private void addElif(@NotNull List<Byte> bytes, int start, @NotNull ElifStatementNode elif, boolean trailingJump, boolean popAs) {
        if (popAs) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        var cond = elif.getTest();
        var body = elif.getBody();
        bytes.add(Bytecode.JUMP.value);
        var jumpTarget = bytes.size();
        bytes.addAll(Util.zeroToBytes());  // Set jump target as temp value
        if (elif.getAs().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + Bytecode.JUMP.size() + bytes.size(), cond, info, 1));
            addBody(bytes, start, body, trailingJump);
        } else {
            bytes.addAll(addAs(start + bytes.size(), cond, elif.getAs()));
            addBodyWithAs(bytes, start, body, trailingJump, elif.getAs().getName());
            info.removeStackFrame();
        }
        // Set jump target
        var target = Util.intToBytes(start + bytes.size() + trailingJumpBytes(trailingJump));
        Util.emplace(bytes, target, jumpTarget);
    }

    private void addElse(@NotNull List<Byte> bytes, int start, StatementBodyNode body, boolean popAs) {
        if (popAs) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP.size(), body, info);
        // Jump index
        var jumpTarget = start + bytes.size() + Bytecode.JUMP.size() + bodyBytes.size();
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }

    private List<Byte> addAs(int start, TestNode condition, VariableNode as) {
        if (!(condition instanceof OperatorNode)) {
            throw CompilerException.of(
                    "Cannot use 'as' here: condition must be 'instanceof' or 'is not null'", condition
            );
        }
        var result = OperatorConverter.convertWithAs(start, (OperatorNode) condition, info, 1);
        var bytes = result.getKey();
        var asType = result.getValue();
        info.addStackFrame();
        info.addVariable(as.getName(), asType);
        return bytes;
    }

    private int trailingJumpBytes(boolean trailingJump) {
        return trailingJump ? Bytecode.JUMP.size() : 0;
    }
}
