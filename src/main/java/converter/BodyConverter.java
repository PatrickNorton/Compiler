package main.java.converter;

import main.java.parser.StatementBodyNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BodyConverter implements BaseConverter {
    private final StatementBodyNode node;
    private final CompilerInfo info;

    public BodyConverter(CompilerInfo info, StatementBodyNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        return convertAndReturn(start).getKey();
    }

    @Override
    @NotNull
    public Pair<List<Byte>, Boolean> convertAndReturn(int start) {
        info.addStackFrame();
        boolean returned = false;
        boolean warned = false;
        List<Byte> bytes = new ArrayList<>();
        for (var statement : node) {
            if (returned && !warned) {
                CompilerWarning.warn("Unreachable statement", WarningType.UNREACHABLE, info, statement);
                warned = true;
            }
            var pair = BaseConverter.bytesWithReturn(start + bytes.size(), statement, info);
            if (pair.getValue()) {
                returned = true;
            }
            bytes.addAll(pair.getKey());
        }
        info.removeStackFrame();
        return Pair.of(bytes, returned);
    }
}
