package main.java.converter;

import main.java.parser.RaiseStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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
        Arrays.fill(result, Builtins.throwsType());
        return result;
    }

    @Override
    @NotNull
    public Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        var divergingInfo = node.getCond().isEmpty()
                ? new DivergingInfo().knownReturn() : new DivergingInfo().possibleReturn();
        return Pair.of(convert(), divergingInfo);
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        var bytes = new BytecodeList();
        if (!node.getFrom().isEmpty()) {
            throw CompilerTodoError.of("'from' clauses in raise statements not supported yet", node);
        }
        if (retCount > 0 && !node.getCond().isEmpty()) {
            throw returnError();
        }
        var condLoc = IfConverter.addJump(bytes, node.getCond(), info);
        if (condLoc != null && retCount > 0) {
            throw CompilerException.of("'raise' statement with if clause is not allowed in expressions", node);
        }
        var converter = TestConverter.of(info, node.getRaised(), 1);
        var retType = converter.returnType()[0];
        if (!Builtins.throwable().isSuperclass(retType)) {
            throw CompilerException.format(
                    "Expected superclass of 'Throwable' in raise statement body, got '%s'",
                    node, retType.name()
            );
        }
        bytes.addAll(converter.convert());
        bytes.add(Bytecode.THROW);  // TODO: THROW_QUICK
        if (condLoc != null) {
            bytes.addLabel(condLoc);
        }
        return bytes;
    }

    private CompilerException returnError() {
        return CompilerException.of("'raise' statement with trailing 'if' does not return values", node);
    }
}
