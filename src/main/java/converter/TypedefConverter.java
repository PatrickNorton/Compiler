package main.java.converter;

import main.java.parser.TypedefStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class TypedefConverter implements BaseConverter {
    private TypedefStatementNode node;
    private CompilerInfo info;

    public TypedefConverter(CompilerInfo info, TypedefStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {  // TODO: Recursive references in typedef
        var type = info.getType(node.getType()).typedefAs(node.getName().strName());
        info.addType((NameableType) type);  // FIXME: Don't require cast
        info.addVariable(type.name(), type);
        return Collections.emptyList();
    }
}
