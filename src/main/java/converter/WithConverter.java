package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.WithStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WithConverter implements BaseConverter {
    private WithStatementNode node;
    private CompilerInfo info;

    public WithConverter(CompilerInfo info, WithStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        assert node.getManaged().size() == 1;
        assert node.getVars().length == 1;
        info.addStackFrame();
        var contextConverter = TestConverter.of(info, node.getManaged().get(0), 1);
        var variable = node.getVars()[0];
        var valueType = node.getVars()[0].getType();
        var returnType = contextConverter.returnType()[0].operatorReturnType(OpSpTypeNode.ENTER);
        var trueType = valueType.isDecided() ? info.getType(valueType) : returnType;
        if (!trueType.isSuperclass(returnType)) {
            throw CompilerException.format(
                    "Object in 'with' statement returns '%s' from operator enter(), " +
                            "attempted to assign it to variable of incompatible type '%s",
                    node, returnType, valueType
            );
        }
        List<Byte> bytes = new ArrayList<>(contextConverter.convert(start));
        info.addVariable(variable.getVariable().getName(), trueType);
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.ENTER.ordinal()));
        bytes.addAll(Util.shortZeroBytes());
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(variable.getVariable().getName())));
        bytes.add(Bytecode.ENTER_TRY.value);
        var tryJump = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getBody(), info));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), tryJump);
        bytes.add(Bytecode.FINALLY.value);
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.EXIT.ordinal()));
        bytes.addAll(Util.shortZeroBytes());
        bytes.add(Bytecode.END_TRY.value);
        bytes.addAll(Util.shortZeroBytes());
        return bytes;
    }
}
