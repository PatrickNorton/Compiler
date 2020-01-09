package main.java.converter;

import main.java.parser.ImportExportNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ImportExportConverter implements BaseConverter {
    private CompilerInfo info;
    private ImportExportNode node;

    public ImportExportConverter(CompilerInfo info, ImportExportNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        switch (node.getType()) {
            case "import":
            case "typeget":
                addImport();
                break;
            case "export":
                addExport();
                break;
            default:
                throw CompilerInternalError.of("Unknown type for ImportExportNode: " + node.getType(), node);
        }
        return Collections.emptyList();
    }

    private void addImport() {
        assert node.getType().equals("import") || node.getType().equals("typeget");
        var from = node.getFrom().toString();
        boolean renamed = node.getAs().length > 0;
        for (int i = 0; i < node.getValues().length; i++) {
            String importName = from + node.getValues()[i];
            int importNumber = info.addImport(from + node.getValues()[i]);
            var constant = new ImportConstant(importNumber);
            info.addConstant(constant);
            var localName = (renamed ? node.getAs() : node.getValues())[i].toString();
            info.addVariable(localName, info.importType(importName));
        }
    }

    private void addExport() {
        assert node.getType().equals("export");
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
