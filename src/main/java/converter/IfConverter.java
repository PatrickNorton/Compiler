package main.java.converter;

import main.java.parser.ElifStatementNode;
import main.java.parser.IfStatementNode;
import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IfConverter implements BaseConverter {
    private final IfStatementNode node;
    private final CompilerInfo info;

    public IfConverter(CompilerInfo info, IfStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes;
        boolean hasAs = !node.getAs().isEmpty();
        boolean hasElse = !node.getElseStmt().isEmpty();
        if (hasAs) {
            bytes = addAs(start, node.getConditional(), node.getAs());
            addBodyWithAs(bytes, start, node.getBody(), node.getAs());
            info.removeStackFrame();
        } else {
            var pair = convertOptimizedNot(start, node.getConditional());
            bytes = new ArrayList<>(pair.getKey());
            boolean trailingJump = node.getElifs().length > 0 || hasElse;
            addBody(bytes, start, node.getBody(), trailingJump, pair.getValue());
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

    private void addBody(
            @NotNull List<Byte> bytes, int start, StatementBodyNode body, boolean trailingJump, boolean jumpType
    ) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP_FALSE.size(), body, info);
        // Jump index
        var jumpTarget = start + bytes.size() + Bytecode.JUMP_FALSE.size() + bodyBytes.size() + trailingJumpBytes(trailingJump);
        bytes.add(jumpType ? Bytecode.JUMP_TRUE.value : Bytecode.JUMP_FALSE.value);
        bytes.addAll(Util.intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }

    private void addBodyWithAs(@NotNull List<Byte> bytes, int start, StatementBodyNode body, VariableNode asName) {
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jumpInsert = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(asName)));
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), body, info));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size() + trailingJumpBytes(true)), jumpInsert);
    }

    private void addElif(@NotNull List<Byte> bytes, int start, @NotNull ElifStatementNode elif, boolean trailingJump, boolean popAs) {
        var cond = elif.getTest();
        var body = elif.getBody();
        bytes.add(Bytecode.JUMP.value);
        var jumpTarget = bytes.size();
        bytes.addAll(Util.zeroToBytes());  // Set jump target as temp value
        if (popAs) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        if (elif.getAs().isEmpty()) {
            var pair = convertOptimizedNot(start + Bytecode.JUMP.size() + bytes.size(), cond);
            bytes.addAll(pair.getKey());
            addBody(bytes, start, body, trailingJump, pair.getValue());
        } else {
            bytes.addAll(addAs(start + bytes.size(), cond, elif.getAs()));
            addBodyWithAs(bytes, start, body, elif.getAs());
            info.removeStackFrame();
        }
        // Set jump target
        var target = Util.intToBytes(start + bytes.size() + trailingJumpBytes(trailingJump));
        Util.emplace(bytes, target, jumpTarget);
    }

    private void addElse(@NotNull List<Byte> bytes, int start, StatementBodyNode body, boolean popAs) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP.size(), body, info);
        // Jump index
        var popAsBytes = popAs ? Bytecode.POP_TOP.value : 0;
        var jumpTarget = start + bytes.size() + Bytecode.JUMP.size() + bodyBytes.size() + popAsBytes;
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(jumpTarget));
        if (popAs) {
            bytes.add(Bytecode.POP_TOP.value);
        }
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
        info.checkDefinition(as.getName(), as);
        info.addVariable(as.getName(), asType, as);
        return bytes;
    }

    @NotNull
    private Pair<List<Byte>, Boolean> convertOptimizedNot(int start, TestNode cond) {
        if (cond instanceof OperatorNode) {
            var op = (OperatorNode) cond;
            if (op.getOperator() == OperatorTypeNode.BOOL_NOT) {
                if (op.getOperands().length != 1) {
                    throw CompilerInternalError.of("Got more than one operand in 'not' statement", cond);
                } else {
                    var bytes = TestConverter.bytes(start, op.getOperands()[0].getArgument(), info, 1);
                    return Pair.of(bytes, true);
                }
            }
        }
        var bytes = TestConverter.bytes(start, cond, info, 1);
        return Pair.of(bytes, false);
    }

    private int trailingJumpBytes(boolean trailingJump) {
        return trailingJump ? Bytecode.JUMP.size() : 0;
    }
}
