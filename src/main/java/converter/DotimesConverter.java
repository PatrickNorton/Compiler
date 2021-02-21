package main.java.converter;

import main.java.parser.DotimesStatementNode;
import org.jetbrains.annotations.NotNull;

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
