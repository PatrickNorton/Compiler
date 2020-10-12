package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class InConverter extends OperatorConverter {
    private final boolean inType;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public InConverter(
            boolean inType,
            ArgumentNode[] args,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.inType = inType;
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.BOOL};
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        if (args.length != 2) {
            throw CompilerException.format("Expected 2 arguments for 'in' operator, got %d", lineInfo, args.length);
        }
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), args[1].getArgument(), info, 1));
        bytes.add(Bytecode.SWAP_2.value);
        bytes.add(Bytecode.CONTAINS.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        } else if (!inType) {
            bytes.add(Bytecode.BOOL_NOT.value);
        }
        return bytes;
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        throw asException(lineInfo);
    }
}
