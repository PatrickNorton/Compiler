package main.java.converter;

import main.java.parser.ImportExportNode;
import org.jetbrains.annotations.NotNull;

public final class ImportExportConverter implements BaseConverter {
    private final CompilerInfo info;
    private final ImportExportNode node;

    public ImportExportConverter(CompilerInfo info, ImportExportNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        switch (node.getType()) {
            case IMPORT, TYPEGET -> addImport();
            case EXPORT -> throw CompilerException.of("Invalid position for export statement", node);
        }
        return new BytecodeList();
    }

    private void addImport() {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        var imports = info.importHandler().addImport(node);
        for (var pair : imports.entrySet()) {
            var constant = new ImportConstant(pair.getValue(), pair.getKey());
            info.addConstant(constant);
            info.checkDefinition(pair.getKey(), node);
            info.addVariable(pair.getKey(), null, constant, node);  // FIXME: Variable should have a type
        }
    }
}
