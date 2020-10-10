package main.java.converter;

import main.java.parser.RaiseStatementNode;
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
        int condLoc;
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            condLoc = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        } else {
            condLoc = -1;
        }
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
}
