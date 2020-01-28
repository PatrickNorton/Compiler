package main.java.converter;

import main.java.parser.StatementBodyNode;

import java.util.ArrayList;
import java.util.List;

public final class BodyConverter implements BaseConverter {
    private StatementBodyNode node;
    private CompilerInfo info;

    public BodyConverter(CompilerInfo info, StatementBodyNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        for (var stmt : node) {
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt, info));
        }
        return bytes;
    }
}
