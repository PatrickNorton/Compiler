package main.java.converter;

import main.java.parser.TypedefStatementNode;
import org.jetbrains.annotations.NotNull;

public final class TypedefConverter implements BaseConverter {
    private final TypedefStatementNode node;
    private final CompilerInfo info;

    public TypedefConverter(CompilerInfo info, TypedefStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public BytecodeList convert() {  // TODO: Recursive references in typedef
        var type = info.getType(node.getType()).typedefAs(node.getName().strName());
        info.addType(type);
        info.checkDefinition(type.name(), node);
        var constant = TypeLoader.typeConstant(node.getLineInfo(), type, info).orElseThrow(
                () -> CompilerException.of("Cannot typedef local types", node)
        );
        info.addVariable(type.name(), type, constant, node);
        return new BytecodeList();
    }
}
