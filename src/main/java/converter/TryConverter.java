package main.java.converter;

import main.java.parser.TryStatementNode;

import java.util.ArrayList;
import java.util.List;

public final class TryConverter implements BaseConverter {
    private final TryStatementNode node;
    private final CompilerInfo info;

    public TryConverter(CompilerInfo info, TryStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.ENTER_TRY.value);
        var jump0 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getBody(), info));
        bytes.add(Bytecode.JUMP.value);
        var jump1 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(bytes.size()), jump0);
        for (var except : node.getExcepted()) {
            bytes.add(Bytecode.EXCEPT_N.value);
            var constIndex = info.constIndex(info.getType(except).name());
            bytes.addAll(Util.shortToBytes(constIndex));
        }
        if (!node.getAsVar().isEmpty()) {
            var asVar = node.getAsVar();
            info.addVariable(asVar.getName(), TypeObject.union(info.typesOf(node.getExcepted())), asVar);
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(info.varIndex(node.getAsVar())));
        } else {
            bytes.add(Bytecode.POP_TOP.value);
        }
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getExcept(), info));
        bytes.add(Bytecode.JUMP.value);
        var jump2 = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        if (!node.getFinallyStmt().isEmpty()) {
            int jump3;
            if (node.getExcepted().length > 0) {
                bytes.add(Bytecode.JUMP.value);
                jump3 = bytes.size();
                bytes.addAll(Util.zeroToBytes());
            } else {
                jump3 = -1;
            }
            bytes.add(Bytecode.FINALLY.value);
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getFinallyStmt(), info));
            if (jump3 != -1) {
                Util.emplace(bytes, Util.intToBytes(bytes.size()), jump2);
            }
            // Work out some kinks first
            throw CompilerTodoError.of("Finally not implemented yet", node.getFinallyStmt());
        }
        Util.emplace(bytes, Util.intToBytes(bytes.size()), jump1);
        Util.emplace(bytes, Util.intToBytes(bytes.size()), jump2);
        bytes.add(Bytecode.END_TRY.value);
        bytes.addAll(Util.intToBytes(node.getExcepted().length));
        return bytes;
    }
}
