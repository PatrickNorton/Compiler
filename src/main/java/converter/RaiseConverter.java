package main.java.converter;

import main.java.parser.RaiseStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RaiseConverter implements TestConverter {
    private final RaiseStatementNode node;
    private final CompilerInfo info;
    private final int retCount;

    public RaiseConverter(CompilerInfo info, RaiseStatementNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        if (retCount > 0 && !node.getCond().isEmpty()) {
            throw returnError();
        }
        var result = new TypeObject[retCount];
        Arrays.fill(result, Builtins.THROWS);
        return result;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        if (!node.getFrom().isEmpty()) {
            throw CompilerTodoError.of("'from' clauses in raise statements not supported yet", node);
        }
        if (retCount > 0 && !node.getCond().isEmpty()) {
            throw returnError();
        }
        int condLoc = IfConverter.addJump(start, bytes, node.getCond(), info);
        var converter = TestConverter.of(info, node.getRaised(), 1);
        var retType = converter.returnType()[0];
        if (!Builtins.THROWABLE.isSuperclass(retType)) {
            throw CompilerException.format(
                    "Expected superclass of 'Throwable' in raise statement body, got '%s'",
                    node, retType.name()
            );
        }
        bytes.addAll(converter.convert(start + bytes.size()));
        bytes.add(Bytecode.THROW.value);  // TODO: THROW_QUICK
        if (condLoc != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), condLoc);
        }
        return bytes;
    }

    @Override
    @NotNull
    public Pair<List<Byte>, DivergingInfo> convertAndReturn(int start) {
        var divergingInfo = node.getCond().isEmpty()
                ? new DivergingInfo().knownReturn() : new DivergingInfo().possibleReturn();
        return Pair.of(convert(start), divergingInfo);
    }

    private CompilerException returnError() {
        return CompilerException.of("'raise' statement with trailing 'if' does not return values", node);
    }
}
