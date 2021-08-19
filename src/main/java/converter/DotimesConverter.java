package main.java.converter;

import main.java.parser.DotimesStatementNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public final class DotimesConverter extends LoopConverter {
    private final DotimesStatementNode node;
    private final CompilerInfo info;

    public DotimesConverter(CompilerInfo info, DotimesStatementNode node) {
        super(info);
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public BytecodeList trueConvert() {
        var countConverter = TestConverter.of(info, node.getIterations(), 1);
        if (!Builtins.intType().isSuperclass(countConverter.returnType()[0])) {
            throw CompilerException.format(
                    "dotimes loop's iteration count has type '%s', which is not a subclass of int",
                    node.getIterations(), countConverter.returnType()[0].name()
            );
        }
        var constant = countConverter.constantReturn();
        if (constant.isPresent()) {
            checkConstant(constant.orElseThrow());
        }
        var bytes = new BytecodeList(countConverter.convert());
        bytes.addLabel(info.loopManager().continueLabel());
        int topLabel = info.newJumpLabel();
        bytes.addLabel(topLabel);
        int label = info.newJumpLabel();
        bytes.add(Bytecode.DOTIMES, label);
        bytes.addAll(convertBody());
        bytes.add(Bytecode.JUMP, topLabel);
        bytes.addLabel(label);
        return bytes;
    }

    private void checkConstant(LangConstant constant) {
        var value = IntArithmetic.convertConst(constant).orElseThrow();
        if (value.signum() < 0) {
            CompilerWarning.warn(
                    "Loop will never execute\n" +
                            "Note: 'dotimes' loops with negative values may become an error in the future"
                    , WarningType.TRIVIAL_VALUE, info, node.getIterations()
            );
        } else if (value.equals(BigInteger.ZERO)) {
            CompilerWarning.warn("Loop will never execute", WarningType.TRIVIAL_VALUE, info, node.getIterations());
        } else if (value.equals(BigInteger.ONE)) {
            CompilerWarning.warn(
                    "'dotimes 1' is unnecessary, as loop will only execute once",
                    WarningType.TRIVIAL_VALUE, info, node.getIterations()
            );
        }
    }

    private BytecodeList convertBody() {
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
        var bytes = pair.getKey();
        var divergingInfo = pair.getValue();
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes no more than once", WarningType.UNREACHABLE, info, node);
        }
        return bytes;
    }
}
