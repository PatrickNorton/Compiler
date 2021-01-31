package main.java.converter;

import main.java.parser.OperatorNode;
import main.java.parser.VariableNode;
import main.java.parser.WhileStatementNode;
import main.java.util.Pair;
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
        return trueConvertWithReturn(start).getKey();
    }

    @Override
    protected Pair<List<Byte>, Boolean> trueConvertWithReturn(int start) {
        List<Byte> bytes = new ArrayList<>();
        boolean hasAs = !node.getAs().isEmpty();
        info.loopManager().setContinuePoint(start);
        boolean isWhileTrue;
        boolean willReturn;
        if (!hasAs) {
            if (node.getCond() instanceof VariableNode && ((VariableNode) node.getCond()).getName().equals("true")) {
                isWhileTrue = true;
            } else {
                isWhileTrue = false;
                var cond = TestConverter.bytes(start, node.getCond(), info, 1);
                bytes.addAll(cond);
            }
        } else {
            isWhileTrue = false;
            convertCondWithAs(bytes, start);
        }
        bytes.add(isWhileTrue ? Bytecode.JUMP.value : Bytecode.JUMP_FALSE.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        if (hasAs) {
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(info.varIndex(node.getAs())));
        }
        var pair = BaseConverter.bytesWithReturn(start + bytes.size(), node.getBody(), info);
        var body = pair.getKey();
        willReturn = pair.getValue();
        bytes.addAll(body);
        if (hasAs) {
            info.removeStackFrame();
        }
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        if (!node.getNobreak().isEmpty()) {
            if (isWhileTrue) {
                CompilerWarning.warn(
                        "'nobreak' statement in a 'while true' loop is unreachable",
                        WarningType.UNREACHABLE, info, node.getNobreak()
                );
            }
            var nobreakReturns = addNobreak(bytes, start, jumpLoc);
            if (!isWhileTrue) {
                willReturn &= nobreakReturns;
            }
        } else if (hasAs) {
            willReturn = false;  // 'while true' cannot have an 'as' clause
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            bytes.add(Bytecode.POP_TOP.value);
        } else {
            willReturn &= isWhileTrue;
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        }
        return Pair.of(bytes, willReturn);
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
        info.addVariable(node.getAs().getName(), pair.getValue(), node.getAs());
        bytes.addAll(pair.getKey());
    }

    private boolean addNobreak(@NotNull List<Byte> bytes, int start, int jumpLoc) {
        var pair = BaseConverter.bytesWithReturn(start + bytes.size(), node.getNobreak(), info);
        bytes.addAll(pair.getKey());
        if (!node.getAs().isEmpty()) {
            int jumpPos = start + bytes.size() + Bytecode.JUMP.size() + Bytecode.POP_TOP.size();
            bytes.add(Bytecode.JUMP.value);
            bytes.addAll(Util.intToBytes(jumpPos));
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            bytes.add(Bytecode.POP_TOP.value);
        }
        return pair.getValue();
    }
}
