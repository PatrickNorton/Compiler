package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.IfStatementNode;
import main.java.parser.WhileStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseConverter {
    List<Byte> convert(int start);

    static List<Byte> bytes(int start, BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convert(start);
    }

    @NotNull
    private static BaseConverter toBytes(@NotNull BaseNode node, CompilerInfo info) {
        if (node instanceof IfStatementNode) {
            return new IfConverter(info, (IfStatementNode) node);
        } else if (node instanceof WhileStatementNode) {
            return new WhileConverter(info, (WhileStatementNode) node);
        } else {
            throw new UnsupportedOperationException("Unsupported node");
        }
    }
}
