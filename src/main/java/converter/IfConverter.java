package main.java.converter;

import main.java.parser.ElifStatementNode;
import main.java.parser.IfStatementNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IfConverter implements BaseConverter {
    private IfStatementNode node;

    public IfConverter(IfStatementNode node) {
        this.node = node;
    }

    @Override
    public List<Byte> convert(int start) {
        var bytes = new ArrayList<>(BaseConverter.bytes(start, node.getConditional()));
        addBody(bytes, start + bytes.size(), node.getBody());
        for (var elif : node.getElifs()) {
            addElif(bytes, start, elif);
        }
        if (!node.getElseStmt().isEmpty()) {
            addElse(bytes, start, node.getElseStmt());
        }
        return bytes;
    }

    private void addBody(@NotNull List<Byte> bytes, int start, StatementBodyNode body) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP_FALSE.size(), body);
        // Jump index
        bytes.add(Bytecode.JUMP_FALSE.value);
        var jumpTarget = start + bytes.size() + bodyBytes.size();
        bytes.addAll(intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }

    private void addElif(@NotNull List<Byte> bytes, int start, @NotNull ElifStatementNode elif) {
        var cond = elif.getTest();
        var body = elif.getBody();
        bytes.add(Bytecode.JUMP.value);
        var jumpTarget = bytes.size();
        bytes.addAll(intToBytes(0));  // Set jump target as temp value
        bytes.addAll(BaseConverter.bytes(start + Bytecode.JUMP.size() + bytes.size(), cond));
        addBody(bytes, start, body);
        // Set jump target
        var target = intToBytes(start + bytes.size());
        for (int i = 0; i < target.size(); i++) {
            bytes.set(jumpTarget + i, target.get(i));
        }
    }

    private void addElse(@NotNull List<Byte> bytes, int start, StatementBodyNode body) {
        var bodyBytes = BaseConverter.bytes(start + bytes.size() + Bytecode.JUMP.size(), body);
        // Jump index
        bytes.add(Bytecode.JUMP.value);
        var jumpTarget = start + bytes.size() + bodyBytes.size();
        bytes.addAll(intToBytes(jumpTarget));
        bytes.addAll(bodyBytes);
    }
}
