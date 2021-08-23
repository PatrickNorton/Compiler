package main.java.converter;

import main.java.parser.VariableNode;
import main.java.util.Levenshtein;
import org.jetbrains.annotations.NotNull;

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
        if (info.variableIsConstant(node.getName())) {
            return Optional.of(info.getConstant(info.constIndex(node.getName())));
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[]{info.getType(node.getName()).orElseThrow(this::nameError)};
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (retCount == 0) {
            checkDef();
            CompilerWarning.warnf("Unused variable %s", WarningType.UNUSED, info, node, node.getName());
            return new BytecodeList();
        } else if (retCount > 1) {
            checkDef();
            throw CompilerException.format("Variable only returns 1 value, expected %d", node, retCount);
        }
        String name = node.getName();
        if (name.equals("null")) {
            return BytecodeList.of(Bytecode.LOAD_NULL);
        }
        checkDef();
        if (info.variableIsStatic(name)) {
            var bytecode = Bytecode.LOAD_STATIC;
            var bytes = new BytecodeList(bytecode.size());
            short index = info.staticVarIndex(node);
            assert index != -1;
            bytes.add(bytecode, index);
            return bytes;
        } else {
            boolean isConst = info.variableIsConstant(name);
            var bytecode = isConst ? Bytecode.LOAD_CONST : Bytecode.LOAD_VALUE;
            var bytes = new BytecodeList(bytecode.size());
            short index = isConst ? info.constIndex(name) : info.varIndex(node);
            assert index != -1;
            bytes.add(bytecode, index);
            return bytes;
        }
    }

    private void checkDef() {
        if (!node.getName().equals("null") && info.varIsUndefined(node.getName())) {
            throw nameError();
        }
    }

    private CompilerException nameError() {
        var name = node.getName();
        var closest = Levenshtein.closestName(name, info.definedNames());
        if (closest.isPresent()) {
            return CompilerException.format(
                    "Variable '%s' not defined.%nDid you mean '%s'?", node, name, closest.orElseThrow()
            );
        } else {
            return CompilerException.format("Variable '%s' not defined", node, name);
        }
    }
}
