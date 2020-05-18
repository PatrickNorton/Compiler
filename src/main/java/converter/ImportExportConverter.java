package main.java.converter;

import main.java.parser.ImportExportNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

public final class ImportExportConverter implements BaseConverter {
    private final CompilerInfo info;
    private final ImportExportNode node;

    public ImportExportConverter(CompilerInfo info, ImportExportNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    @Unmodifiable
    public List<Byte> convert(int start) {
        switch (node.getType()) {
            case IMPORT:
            case TYPEGET:
                addImport();
                break;
            case EXPORT:
                addExport();
                break;
            default:
                throw CompilerInternalError.of("Unknown type for ImportExportNode: " + node.getType(), node);
        }
        return Collections.emptyList();
    }

    private void addImport() {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        var from = node.getFrom().toString();
        boolean renamed = node.getAs().length > 0;
        for (int i = 0; i < node.getValues().length; i++) {
            String importName = from + "." + node.getValues()[i];
            int importNumber = info.addImport(importName);
            var localName = (renamed ? node.getAs() : node.getValues())[i].toString();
            var constant = new ImportConstant(importNumber, localName);
            info.addConstant(constant);
            info.checkDefinition(localName, node.getValues()[i]);
            info.addVariable(localName, info.importType(importName), constant, node.getValues()[i]);
        }
    }

    private void addExport() {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean hasAs = node.getAs().length > 0;
        if (!node.getFrom().isEmpty()) {
            var from = node.getFrom().toString();
            for (int i = 0; i < node.getValues().length; i++) {
                info.addImport(from + node.getValues()[i]);
                var exportedName = hasAs ? node.getAs()[i].toString() : node.getValues()[i].toString();
                info.addExport(exportedName, info.importType(from + node.getValues()[i]), node.getLineInfo());
            }
        } else {
            for (int i = 0; i < node.getValues().length; i++) {
                var exportedName = hasAs ? node.getAs()[i].toString() : node.getValues()[i].toString();
                info.addExport(exportedName, info.getType(node.getValues()[i].toString()), node.getLineInfo());
            }
        }
    }
}
