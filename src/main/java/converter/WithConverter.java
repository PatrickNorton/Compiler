package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.WithStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WithConverter implements BaseConverter {
    private final WithStatementNode node;
    private final CompilerInfo info;

    public WithConverter(CompilerInfo info, WithStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (node.getManaged().size() != 1) {
            throw CompilerTodoError.format(
                    "With statements with more than one managed value (got %d)", node, node.getManaged().size()
            );
        } else if (node.getVars().length != 1) {
            throw CompilerTodoError.format(
                    "With statements with more than one managed value (got %d)", node, node.getVars().length
            );
        }
        info.addStackFrame();
        var contextConverter = TestConverter.of(info, node.getManaged().get(0), 1);
        var variable = node.getVars()[0];
        var valueType = node.getVars()[0].getType();
        var returnType = contextConverter.returnType()[0].tryOperatorReturnType(node, OpSpTypeNode.ENTER, info)[0];
        var trueType = valueType.isDecided() ? info.getType(valueType) : returnType;
        if (!trueType.isSuperclass(returnType)) {
            throw CompilerException.format(
                    "Object in 'with' statement returns '%s' from operator enter(), " +
                            "attempted to assign it to variable of incompatible type '%s",
                    node, returnType.name(), trueType.name()
            );
        }
        var bytes = new BytecodeList(contextConverter.convert());
        info.checkDefinition(variable.getVariable().getName(), variable);
        info.addVariable(variable.getVariable().getName(), trueType, variable);
        bytes.add(Bytecode.DUP_TOP);
        bytes.add(Bytecode.CALL_OP, OpSpTypeNode.ENTER.ordinal(), 0);
        bytes.add(Bytecode.STORE, info.varIndex(variable.getVariable()));
        var tryJump = info.newJumpLabel();
        bytes.add(Bytecode.ENTER_TRY, tryJump);
        bytes.addAll(BaseConverter.bytes(node.getBody(), info));
        bytes.addLabel(tryJump);
        bytes.add(Bytecode.FINALLY);
        bytes.add(Bytecode.CALL_OP, OpSpTypeNode.EXIT.ordinal(), 0);
        bytes.add(Bytecode.END_TRY, 0);
        return bytes;
    }
}
