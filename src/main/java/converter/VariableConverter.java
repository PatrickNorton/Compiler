package main.java.converter;

import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VariableConverter implements TestConverter {
    private CompilerInfo info;
    private VariableNode node;
    private int retCount;

    public VariableConverter(CompilerInfo info, VariableNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public TypeObject returnType() {
        if (info.varIsUndefined(node.getName())) {
            throw CompilerException.format("Variable '%s' not defined", node, node.getName());
        }
        return info.getType(node.getName());
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            return Collections.emptyList();
        }
        assert retCount == 1;
        String name = node.getName();
        if (name.equals("null")) {
            return List.of(Bytecode.LOAD_NULL.value);
        }
        if (info.varIsUndefined(node.getName())) {
            throw CompilerException.format("Variable '%s' not defined", node, node.getName());
        }
        boolean isConst = info.variableIsConstant(node.getName());
        var bytecode = isConst ? Bytecode.LOAD_CONST : Bytecode.LOAD_VALUE;
        List<Byte> bytes = new ArrayList<>(bytecode.size());
        bytes.add(bytecode.value);
        short index = isConst ? info.constIndex(name) : info.varIndex(name);
        bytes.addAll(Util.shortToBytes(index));
        return bytes;
    }
}
