package main.java.converter;

import main.java.parser.DeclarationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

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
    @Unmodifiable
    public List<Byte> convert(int start) {
        if (!node.getType().isDecided()) {
            throw CompilerException.of("var not allowed in declarations", node);
        }
        var name = node.getName().getName();
        info.checkDefinition(name, node);
        info.addVariable(name, info.getType(node.getType()), node);
        return Collections.emptyList();
    }
}
