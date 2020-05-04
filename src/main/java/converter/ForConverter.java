package main.java.converter;

import main.java.parser.ForStatementNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TypedVariableNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ForConverter extends LoopConverter {
    private final ForStatementNode node;
    private final CompilerInfo info;

    public ForConverter(CompilerInfo info, ForStatementNode node) {
        super(info);
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public List<Byte> trueConvert(int start) {
        assert node.getVars().length == 1 : "'for' loops with multiple vars not yet implemented";
        assert node.getIterables().size() == 1 : "'for' loops with multiple vars not yet implemented";
        var valueConverter = TestConverter.of(info, node.getIterables().get(0), 1);
        var iteratorType = getIteratorType();
        var variable = node.getVars()[0].getVariable();
        var iteratedName = variable.toString();
        if (node.getVars()[0] instanceof TypedVariableNode) {
            if (Builtins.FORBIDDEN_NAMES.contains(iteratedName)) {
                throw CompilerException.format("Illegal name for variable '%s'", variable, iteratedName);
            }
            info.checkDefinition(iteratedName, node.getVars()[0]);
            info.addVariable(iteratedName, iteratorType, node.getVars()[0]);
        }
        var valueReturnedType = valueConverter.returnType()[0].operatorReturnType(OpSpTypeNode.ITER, info)[0];
        if (!iteratorType.isSuperclass(valueReturnedType)) {
            throw CompilerException.format(
                    "'for'-loop iterator returns '%s', variable requires '%s'",
                    node.getIterables().get(0), valueReturnedType.name(), iteratorType.name()
            );
        }
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("iter"))));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        info.setContinuePoint(start + bytes.size());
        bytes.add(Bytecode.FOR_ITER.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(info.varIndex(iteratedName)));
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getBody(), info));
        bytes.add(Bytecode.JUMP.value);
        info.addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getNobreak(), info));
        return bytes;
    }

    private TypeObject getIteratorType() {
        if (node.getVars()[0] instanceof VariableNode) {
            return info.getType(node.getVars()[0].getVariable().getName());
        }
        var iteratorType = node.getVars()[0].getType();
        if (iteratorType.isDecided()) {
            return info.getType(iteratorType);
        } else {
            var valueConverter = TestConverter.of(info, node.getIterables().get(0), 1);
            return valueConverter.returnType()[0].operatorReturnType(OpSpTypeNode.ITER, info)[0];
        }
    }
}
