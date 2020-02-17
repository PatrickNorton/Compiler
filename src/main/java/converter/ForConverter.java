package main.java.converter;

import main.java.parser.ForStatementNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ForConverter extends LoopConverter {
    private ForStatementNode node;
    private CompilerInfo info;

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
        var iteratedName = node.getVars()[0].getVariable().toString();
        info.addVariable(iteratedName, iteratorType);
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
        var iteratorType = node.getVars()[0].getType();
        if (iteratorType.isDecided()) {
            return info.getType(iteratorType);
        } else {
            var valueConverter = TestConverter.of(info, node.getIterables().get(0), 1);
            return valueConverter.returnType().operatorReturnType(OpSpTypeNode.ITER);
        }
    }
}
