package main.java.converter;

import main.java.parser.FunctionDefinitionNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FunctionDefinitionConverter implements BaseConverter {
    private CompilerInfo info;
    private FunctionDefinitionNode node;

    public FunctionDefinitionConverter(CompilerInfo info, FunctionDefinitionNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        int index = info.addFunction(bytes);
        var constVal = new FunctionConstant(index);
        info.addVariable(node.getName().getName(), Builtins.CALLABLE, constVal);
        info.addStackFrame();
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()));
        }
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        return Collections.emptyList();
    }
}
