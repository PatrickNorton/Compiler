package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.IfStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseConverter {
    int INT_SIZE = Integer.SIZE / Byte.SIZE;

    List<Byte> convert(int start);

    default List<Byte> intToBytes(int value) {
        return List.of(
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        );
    }

    static List<Byte> bytes(int start, BaseNode tokens) {
        return toBytes(tokens).convert(start);
    }

    @NotNull
    private static BaseConverter toBytes(@NotNull BaseNode node) {
        if (node instanceof IfStatementNode) {
            return new IfConverter((IfStatementNode) node);
        } else {
            throw new UnsupportedOperationException("Unsupported node");
        }
    }
}
