package main.java.converter;

import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.OperatorNode;
import main.java.parser.VariableNode;
import main.java.parser.WhileStatementNode;
import main.java.util.OptionalBool;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

public final class WhileConverter extends LoopConverter {
    private final WhileStatementNode node;

    public WhileConverter(CompilerInfo info, WhileStatementNode node) {
        super(info);
        this.node = node;
    }

    @NotNull
    @Override
    protected BytecodeList trueConvert() {
        return trueConvertWithReturn().getKey();
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, DivergingInfo> trueConvertWithReturn() {
        var bytes = new BytecodeList();
        boolean hasAs = !node.getAs().isEmpty();
        bytes.addLabel(info.loopManager().continueLabel());
        var condPair = convertCond(bytes, hasAs);
        var isWhileTrue = condPair.getKey();
        var constantCondition = condPair.getValue();
        var jumpLbl = info.newJumpLabel();
        if (constantCondition.isPresent()) {
            if (!constantCondition.orElseThrow()) {
                bytes.add(Bytecode.JUMP, jumpLbl);
            }
        } else {
            bytes.add(Bytecode.JUMP_FALSE, jumpLbl);
        }
        if (hasAs) {
            bytes.add(Bytecode.STORE, new VariableBytecode(info.varIndex(node.getAs())));
        }
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
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
        bytes.add(Bytecode.JUMP, info.loopManager().continueLabel());
        if (!node.getNobreak().isEmpty()) {
            if (isWhileTrue) {
                CompilerWarning.warn(
                        "'nobreak' statement in a 'while true' loop is unreachable",
                        WarningType.UNREACHABLE, info, node.getNobreak()
                );
            }
            var nobreakReturns = addNobreak(bytes, jumpLbl, !constantCondition.isTrue());
            if (!isWhileTrue) {
                willReturn.andWith(nobreakReturns);
            }
        } else if (hasAs) {
            willReturn.makeUncertain();  // 'while true' cannot have an 'as' clause
            if (!constantCondition.isTrue()) {
                bytes.addLabel(jumpLbl);
            }
            bytes.add(Bytecode.POP_TOP);
        } else if (!isWhileTrue) {
            willReturn.makeUncertain();
            if (!constantCondition.isTrue()) {
                bytes.addLabel(jumpLbl);
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

    @NotNull
    private Pair<Boolean, OptionalBool> convertCond(BytecodeList bytes, boolean hasAs) {
        if (!hasAs) {
            if (node.getCond() instanceof VariableNode var && var.getName().equals("true")) {
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
                    var cond = converter.convert();
                    bytes.addAll(cond);
                    return Pair.of(false, OptionalBool.empty());
                }
            }
        } else {
            convertCondWithAs(bytes);
            return Pair.of(false, OptionalBool.empty());
        }
    }

    private void convertCondWithAs(BytecodeList bytes) {
        var condition = node.getCond();
        if (!(condition instanceof OperatorNode)) {
            throw CompilerException.of(
                    "Cannot use 'as' here: condition must be 'instanceof' or 'is not null'", condition
            );
        }
        var pair = OperatorConverter.convertWithAs((OperatorNode) condition, info, 1);
        info.addStackFrame();
        info.addVariable(node.getAs().getName(), pair.getValue(), node.getAs());
        bytes.addAll(pair.getKey());
    }

    private DivergingInfo addNobreak(@NotNull BytecodeList bytes, Label jumpLbl, boolean emplaceJump) {
        var pair = BaseConverter.bytesWithReturn(node.getNobreak(), info);
        bytes.addAll(pair.getKey());
        if (!node.getAs().isEmpty()) {
            var label = info.newJumpLabel();
            bytes.add(Bytecode.JUMP, jumpLbl);
            if (emplaceJump) {
                bytes.addLabel(jumpLbl);
            }
            bytes.add(Bytecode.POP_TOP);
            bytes.addLabel(label);
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
