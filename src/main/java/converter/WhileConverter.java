package main.java.converter;

import main.java.parser.OperatorNode;
import main.java.parser.VariableNode;
import main.java.parser.WhileStatementNode;
import main.java.util.OptionalBool;
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
    protected Pair<List<Byte>, DivergingInfo> trueConvertWithReturn(int start) {
        List<Byte> bytes = new ArrayList<>();
        boolean hasAs = !node.getAs().isEmpty();
        info.loopManager().setContinuePoint(start);
        var condPair = convertCond(bytes, start, hasAs);
        var isWhileTrue = condPair.getKey();
        var constantCondition = condPair.getValue();
        int jumpLoc;
        if (constantCondition.isPresent()) {
            if (!constantCondition.orElseThrow()) {
                bytes.add(Bytecode.JUMP.value);
                jumpLoc = bytes.size();
                bytes.addAll(Util.zeroToBytes());
            } else {
                jumpLoc = -1;
            }
        } else {
            bytes.add(Bytecode.JUMP_FALSE.value);
            jumpLoc = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        }
        if (hasAs) {
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(info.varIndex(node.getAs())));
        }
        var pair = BaseConverter.bytesWithReturn(start + bytes.size(), node.getBody(), info);
        var body = pair.getKey();
        var willReturn = pair.getValue();
        if ((willReturn.willBreak() || willReturn.willReturn()) && !willReturn.mayContinue()) {
            var msg = isWhileTrue ? "exactly" : "no more than";
            CompilerWarning.warnf("Loop executes %s once", WarningType.UNREACHABLE, info, node, msg);
        }
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
            var nobreakReturns = addNobreak(bytes, start, jumpLoc, !constantCondition.isTrue());
            if (!isWhileTrue) {
                willReturn.andWith(nobreakReturns);
            }
        } else if (hasAs) {
            willReturn.makeUncertain();  // 'while true' cannot have an 'as' clause
            if (!constantCondition.isTrue()) {
                Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            }
            bytes.add(Bytecode.POP_TOP.value);
        } else if (!isWhileTrue) {
            willReturn.makeUncertain();
            if (!constantCondition.isTrue()) {
                Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            }
        }
        if (isWhileTrue && !willReturn.mayBreak()) {
            if (!willReturn.mayReturn() && !info.getFnReturns().isGenerator()) {
                // Generators may infinitely yield, so no warnings for them
                // In the future, we may want to keep track of yields too, so
                // we can warn on yield-less infinite loops
                CompilerWarning.warn("Infinite loop", WarningType.INFINITE_LOOP, info, node);
            } else if (willReturn.mayReturn()) {
                willReturn.knownReturn();
            }
        }
        return Pair.of(bytes, willReturn);
    }

    private Pair<Boolean, OptionalBool> convertCond(List<Byte> bytes, int start, boolean hasAs) {
        if (!hasAs) {
            if (node.getCond() instanceof VariableNode && ((VariableNode) node.getCond()).getName().equals("true")) {
                return Pair.of(true, OptionalBool.of(true));
            } else {
                var converter = TestConverter.of(info, node.getCond(), 1);
                var constant = constantBool(converter);
                if (constant.isPresent()) {
                    CompilerWarning.warnf(
                            "While loop condition always evaluates to %s",
                            WarningType.TRIVIAL_VALUE, info, node.getCond(), constant.orElseThrow()
                    );
                    return Pair.of(false, constant);
                } else {
                    var cond = converter.convert(start + bytes.size());
                    bytes.addAll(cond);
                    return Pair.of(false, OptionalBool.empty());
                }
            }
        } else {
            convertCondWithAs(bytes, start);
            return Pair.of(false, OptionalBool.empty());
        }
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

    private DivergingInfo addNobreak(@NotNull List<Byte> bytes, int start, int jumpLoc, boolean emplaceJump) {
        var pair = BaseConverter.bytesWithReturn(start + bytes.size(), node.getNobreak(), info);
        bytes.addAll(pair.getKey());
        if (!node.getAs().isEmpty()) {
            int jumpPos = start + bytes.size() + Bytecode.JUMP.size() + Bytecode.POP_TOP.size();
            bytes.add(Bytecode.JUMP.value);
            bytes.addAll(Util.intToBytes(jumpPos));
            if (emplaceJump) {
                Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
            }
            bytes.add(Bytecode.POP_TOP.value);
        }
        return pair.getValue();
    }

    private static OptionalBool constantBool(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            return constant.orElseThrow().boolValue();
        } else {
            return OptionalBool.empty();
        }
    }
}
