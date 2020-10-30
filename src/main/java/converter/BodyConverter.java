package main.java.converter;

import main.java.parser.StatementBodyNode;

import java.util.ArrayList;
import java.util.List;

public final class BodyConverter implements BaseConverter {
    private final StatementBodyNode node;
    private final CompilerInfo info;

    public BodyConverter(CompilerInfo info, StatementBodyNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        info.addStackFrame();
        List<Byte> bytes = new ArrayList<>();
        for (var stmt : node) {
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt, info));
        }
        info.removeStackFrame();
        return bytes;
    }
}
