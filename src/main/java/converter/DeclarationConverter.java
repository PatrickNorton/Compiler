package main.java.converter;

import main.java.parser.DeclarationNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class DeclarationConverter implements BaseConverter {
    private DeclarationNode node;
    private CompilerInfo info;

    public DeclarationConverter(CompilerInfo info, DeclarationNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        info.addVariable(node.getName().getName(), info.getType(node.getType()));
        return Collections.emptyList();
    }
}
