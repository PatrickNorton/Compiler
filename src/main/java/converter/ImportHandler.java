package main.java.converter;

import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ImportHandler {
    private final Set<String> exports = new HashSet<>();
    private final Map<String, TypeObject> exportTypes = new HashMap<>();
    private final IndexedSet<String> imports = new IndexedHashSet<>();
    private final Map<String, TypeObject> importTypes = new HashMap<>();

    private boolean allowSettingExports = false;

    /**
     * Adds an export.
     * <p>
     *     If setting exports is not allowed, (e.g. this is not in the process
     *     of {@link CompilerInfo#link linking}), a {@link CompilerException}
     *     will be thrown. This is not a {@link CompilerInternalError} because
     *     it is most likely to be thrown due to an illegally-placed {@link
     *     ImportExportNode 'export'} statement, as opposed to a compiler bug.
     * </p>
     *
     * @see CompilerInfo#link
     * @param name The name of the export
     * @param type The type of the export
     * @param info The {@link LineInfo} for the export statement
     */
    public void addExport(String name, TypeObject type, LineInfo info) {
        if (!allowSettingExports) {
            throw CompilerException.of("Illegal position for export statement", info);
        }
        this.exports.add(name);
        exportTypes.put(name, type);
    }

    /**
     * Gets the type of an export.
     *
     * @param name The name of the export
     * @return The export type
     */
    public TypeObject exportType(String name) {
        return exportTypes.get(name);
    }

    /**
     * Gets the set of names exported by this file.
     *
     * @return The set of exports
     */
    public Set<String> getExports() {
        return exports;
    }

    /**
     * Adds an import.
     *
     * @param name The name if the import
     * @return The index of the import in the imports set
     */
    public int addImport(@NotNull String name) {
        var names = name.split("\\.");
        if (!imports.contains(name)) {
            var file = Converter.resolveFile(names[0]);
            CompilerInfo f = Converter.findModule(names[0]).compile(file);
            imports.add(name);
            importTypes.put(name, f.importHandler().exportTypes.get(names[1]));
        }
        return imports.indexOf(name);
    }

    public IndexedSet<String> getImports() {
        return imports;
    }

    /**
     * Gets the type of an import.
     *
     * @param name The name of the import
     * @return THe type of the import
     */
    public TypeObject importType(String name) {
        return importTypes.get(name);
    }

    /**
     * Adds imports/exports from the {@link Linker} given.
     * <p>
     *     This assumes it will only be called once, and will overwrite all
     *     previous imports from the linker.
     * </p>
     *
     * @param linker The linker to get the information from
     */
    public void setFromLinker(@NotNull Linker linker) {
        var exports = linker.getExports();
        var globals = linker.getGlobals();
        try {
            allowSettingExports = true;
            for (var entry : exports.entrySet()) {
                var exportName = entry.getValue().getKey();
                var exportType = globals.get(entry.getKey());
                if (exportType == null) {
                    var lineInfo = entry.getValue().getValue();
                    throw CompilerException.of("Undefined name for export: " + exportName, lineInfo);
                }
                this.exports.add(exportName);
                this.exportTypes.put(exportName, exportType);
            }
        } finally {
            allowSettingExports = false;
        }
    }
}
