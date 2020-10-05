package main.java.converter;

import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class VariableConverter implements TestConverter {
    private final CompilerInfo info;
    private final VariableNode node;
    private final int retCount;

    public VariableConverter(CompilerInfo info, VariableNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        // Check for 'null' at the moment b/c serializing NullConstant fails
        if (!node.getName().equals("null") && info.variableIsConstant(node.getName())) {
            return Optional.of(info.getConstant(info.constIndex(node.getName())));
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        if (info.varIsUndefined(node.getName())) {
            throw CompilerException.format("Variable '%s' not defined", node, node.getName());
        }
        return new TypeObject[]{info.getType(node.getName())};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            if (!node.getName().equals("null") && info.varIsUndefined(node.getName())) {
                throw CompilerException.format("Variable '%s' not defined", node, node.getName());
            }
            CompilerWarning.warnf("Unused variable %s", node, node.getName());
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
        if (info.variableIsStatic(node.getName())) {
            var bytecode = Bytecode.LOAD_STATIC;
            List<Byte> bytes = new ArrayList<>(bytecode.size());
            bytes.add(bytecode.value);
            short index = info.staticVarIndex(node);
            bytes.addAll(Util.shortToBytes(index));
            return bytes;
        } else {
            boolean isConst = info.variableIsConstant(node.getName());
            var bytecode = isConst ? Bytecode.LOAD_CONST : Bytecode.LOAD_VALUE;
            List<Byte> bytes = new ArrayList<>(bytecode.size());
            bytes.add(bytecode.value);
            short index = isConst ? info.constIndex(name) : info.varIndex(node);
            bytes.addAll(Util.shortToBytes(index));
            return bytes;
        }
    }
}
