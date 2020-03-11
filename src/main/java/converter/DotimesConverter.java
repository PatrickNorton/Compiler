package main.java.converter;

import main.java.parser.DotimesStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DotimesConverter extends LoopConverter {
    private DotimesStatementNode node;
    private CompilerInfo info;

    public DotimesConverter(CompilerInfo info, DotimesStatementNode node) {
        super(info);
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> trueConvert(int start) {
        var countConverter = TestConverter.of(info, node.getIterations(), 1);
        if (!Builtins.INT.isSuperclass(countConverter.returnType()[0])) {
            throw CompilerException.format(
                    "dotimes loop's iteration count has type '%s', which is not a subclass of int",
                    node.getIterations(), countConverter.returnType()[0].name()
            );
        }
        List<Byte> bytes = new ArrayList<>(countConverter.convert(start));
        int topJump = start + bytes.size();
        info.setContinuePoint(topJump);
        bytes.add(Bytecode.DOTIMES.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getBody(), info));
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.intToBytes(topJump));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        return bytes;
    }
}
