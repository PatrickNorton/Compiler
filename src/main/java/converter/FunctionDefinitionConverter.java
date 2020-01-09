package main.java.converter;

import main.java.parser.FunctionDefinitionNode;

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

    @Override
    public List<Byte> convert(int start) {
        info.addStackFrame();
        List<Byte> bytes = new ArrayList<>();
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        info.addFunction(node.getName().getName(), bytes);
        return Collections.emptyList();
    }
}
