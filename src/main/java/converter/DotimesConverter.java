package main.java.converter;

import main.java.parser.DotimesStatementNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
    public List<Byte> trueConvert(int start) {
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
        List<Byte> bytes = new ArrayList<>(countConverter.convert(start));
        int topJump = start + bytes.size();
        info.loopManager().setContinuePoint(topJump);
        bytes.add(Bytecode.DOTIMES.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(convertBody(start + bytes.size()));
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(topJump));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        return bytes;
    }

    private void checkConstant(LangConstant constant) {
        var value = IntArithmetic.convertConst(constant).orElseThrow();
        if (value.equals(BigInteger.ZERO)) {
            CompilerWarning.warn("Loop will never execute", WarningType.TRIVIAL_VALUE, info, node.getIterations());
        } else if (value.equals(BigInteger.ONE)) {
            CompilerWarning.warn(
                    "'dotimes 1' is unnecessary, as loop will only execute once",
                    WarningType.TRIVIAL_VALUE, info, node.getIterations()
            );
        }
    }

    private List<Byte> convertBody(int start) {
        var pair = BaseConverter.bytesWithReturn(start, node.getBody(), info);
        var bytes = pair.getKey();
        var divergingInfo = pair.getValue();
        if ((divergingInfo.willBreak() || divergingInfo.willReturn()) && !divergingInfo.mayContinue()) {
            CompilerWarning.warn("Loop executes no more than once", WarningType.UNREACHABLE, info, node);
        }
        return bytes;
    }
}
