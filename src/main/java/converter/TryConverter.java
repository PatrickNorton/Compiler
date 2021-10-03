package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.TryStatementNode;
import org.jetbrains.annotations.NotNull;

public final class TryConverter implements BaseConverter {
    private final TryStatementNode node;
    private final CompilerInfo info;

    public TryConverter(CompilerInfo info, TryStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        var bytes = new BytecodeList();
        var jump0 = info.newJumpLabel();
        bytes.add(Bytecode.ENTER_TRY, jump0);
        bytes.addAll(BaseConverter.bytes(node.getBody(), info));
        var jump1 = info.newJumpLabel();
        bytes.add(Bytecode.JUMP, jump1);
        bytes.addLabel(jump0);
        for (var except : node.getExcepted()) {
            CompilerWarning.warn("Exceptions are very broken right now", WarningType.TODO, info, except);
            bytes.loadConstant(info.getConstant(except.strName()), info);
        }
        bytes.add(Bytecode.EXCEPT_N, new ArgcBytecode((short) node.getExcepted().length));
        if (!node.getAsVar().isEmpty()) {
            var asVar = node.getAsVar();
            info.addVariable(asVar.getName(), TypeObject.union(info.typesOf(node.getExcepted())), asVar);
            bytes.add(Bytecode.STORE, new VariableBytecode(info.varIndex(node.getAsVar())));
        } else {
            bytes.add(Bytecode.POP_TOP);
        }
        bytes.addAll(BaseConverter.bytes(node.getExcept(), info));
        bytes.add(Bytecode.JUMP, jump1);
        if (!node.getFinallyStmt().isEmpty()) {
            convertFinally(bytes);
        }
        bytes.addLabel(jump1);
        bytes.add(Bytecode.END_TRY, new ArgcBytecode((short) node.getExcepted().length));
        return bytes;
    }

    private void convertFinally(BytecodeList bytes) {
        assert !node.getFinallyStmt().isEmpty();
        Label jump3;
        if (node.getExcepted().length > 0) {
            jump3 = info.newJumpLabel();
            bytes.add(Bytecode.JUMP, jump3);
        } else {
            jump3 = null;
        }
        bytes.add(Bytecode.FINALLY);
        var finallyResult = BaseConverter.bytesWithReturn(node.getFinallyStmt(), info);
        bytes.addAll(finallyResult.getKey());
        if (finallyResult.getValue().mayDiverge()) {
            CompilerWarning.warn(
                    "'return', 'break', or 'continue' statements in a " +
                            "finally' statement can cause unexpected behavior",
                    WarningType.NO_TYPE, info, node.getFinallyStmt()
            );
        }
        if (jump3 != null) {
            bytes.addLabel(jump3);
        }
        // Work out some kinks first
        throw CompilerTodoError.of("Finally not implemented yet", node.getFinallyStmt());
    }
}
