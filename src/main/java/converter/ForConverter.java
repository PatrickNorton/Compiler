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
        // assert node.getVars().length == 1 : "'for' loops with multiple vars not yet implemented";
        // assert node.getIterables().size() == 1 : "'for' loops with multiple vars not yet implemented";
        var varLen = node.getVars().length;
        var iterLen = node.getIterables().size();
        if (iterLen > varLen) {
            throw CompilerException.of("For-loops may not have more iterables than variables", node);
        } else if (iterLen == 1) {
            return convertSingleIter(start);
        } else {
            return convertMultipleIter(start);
        }
    }

    @NotNull
    private List<Byte> convertSingleIter(int start) {
        var retCount = node.getVars().length;
        var valueConverter = TestConverter.of(info, node.getIterables().get(0), retCount);
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("iter"))));
        bytes.addAll(valueConverter.convert(start));
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        info.loopManager().setContinuePoint(start + bytes.size());
        bytes.add(Bytecode.FOR_ITER.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.addAll(Util.shortToBytes((short) retCount));
        for (int i = 0; i < node.getVars().length; i++) {
            var typedVar = node.getVars()[i];
            var iteratorType = getIteratorType(i, valueConverter);
            var variable = typedVar.getVariable();
            var iteratedName = variable.toString();
            if (typedVar instanceof TypedVariableNode) {
                if (Builtins.FORBIDDEN_NAMES.contains(iteratedName)) {
                    throw CompilerException.format("Illegal name for variable '%s'", variable, iteratedName);
                }
                info.checkDefinition(iteratedName, typedVar);
                info.addVariable(iteratedName, iteratorType, typedVar);
            }
            bytes.add(Bytecode.STORE.value);
            bytes.addAll(Util.shortToBytes(info.varIndex(iteratedName)));
        }
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getBody(), info));
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addContinue(start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), node.getNobreak(), info));
        return bytes;
    }

    private List<Byte> convertMultipleIter(int start) {
        var varLen = node.getVars().length;
        var iterLen = node.getIterables().size();
        if (varLen != iterLen) {
            throw CompilerException.of(
                    "For loops with more than one iterable must have an equal number of variables and iterables",
                    node
            );
        }
        throw CompilerException.of("Not yet implemented", node);
    }

    private TypeObject getIteratorType(int i, TestConverter valueConverter) {
        if (node.getVars()[i] instanceof VariableNode) {
            return info.getType(node.getVars()[i].getVariable().getName());
        }
        var iteratorType = node.getVars()[i].getType();
        if (iteratorType.isDecided()) {
            return info.getType(iteratorType);
        } else {
            return valueConverter.returnType()[0].operatorReturnType(OpSpTypeNode.ITER, info)[i];
        }
    }
}
