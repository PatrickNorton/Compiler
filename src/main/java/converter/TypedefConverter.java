package main.java.converter;

import main.java.parser.TypedefStatementNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

public final class TypedefConverter implements BaseConverter {
    private final TypedefStatementNode node;
    private final CompilerInfo info;

    public TypedefConverter(CompilerInfo info, TypedefStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    @Unmodifiable
    public List<Byte> convert(int start) {  // TODO: Recursive references in typedef
        var type = info.getType(node.getType()).typedefAs(node.getName().strName());
        info.addType((NameableType) type);  // FIXME: Don't require cast
        info.checkDefinition(type.name(), node);
        info.addVariable(type.name(), type, node);
        return Collections.emptyList();
    }
}
