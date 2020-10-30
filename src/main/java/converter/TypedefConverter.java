package main.java.converter;

import main.java.parser.TypedefStatementNode;

import java.util.Collections;
import java.util.List;

public final class TypedefConverter implements BaseConverter {
    private final TypedefStatementNode node;
    private final CompilerInfo info;

    public TypedefConverter(CompilerInfo info, TypedefStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @Override

    public List<Byte> convert(int start) {  // TODO: Recursive references in typedef
        var type = info.getType(node.getType()).typedefAs(node.getName().strName());
        info.addType(type);
        info.checkDefinition(type.name(), node);
        info.addVariable(type.name(), type, node);
        return Collections.emptyList();
    }
}
