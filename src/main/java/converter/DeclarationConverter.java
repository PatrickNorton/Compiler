package main.java.converter;

import main.java.parser.DeclarationNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class DeclarationConverter implements BaseConverter {
    private final DeclarationNode node;
    private final CompilerInfo info;

    public DeclarationConverter(CompilerInfo info, DeclarationNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (!node.getType().isDecided()) {
            throw CompilerException.of("var not allowed in declarations", node);
        }
        info.addVariable(node.getName().getName(), info.getType(node.getType()));
        return Collections.emptyList();
    }
}
