package main.java.converter;

import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileInfo {
    private Set<String> exports;
    private Map<String, TypeObject> exportTypes;
    private IndexedSet<String> imports;
    private Map<String, TypeObject> importTypes;
    private Map<String, List<Byte>> functions;
    private IndexedSet<LangConstant> constants;

    public FileInfo() {
        this.exports = new HashSet<>();
        this.exportTypes = new HashMap<>();
        this.imports = new IndexedHashSet<>();
        this.importTypes = new HashMap<>();
        this.functions = new LinkedHashMap<>();
        this.constants = new IndexedHashSet<>();
    }

    public void addExport(String name, TypeObject type) {
        this.exports.add(name);
        exportTypes.put(name, type);
    }

    public TypeObject exportType(String name) {
        return exportTypes.get(name);
    }

    public int addImport(String name) {
        if (!imports.contains(name)) {
            FileInfo f = Converter.findModule(name);
            imports.add(name);
            importTypes.put(name, f.exportTypes.get(name));
        }
        return imports.indexOf(name);
    }

    public TypeObject importType(String name) {
        return importTypes.get(name);
    }

    public void addFunction(String name, List<Byte> bytecode) {
        this.functions.put(name, bytecode);
    }

    public int addConstant(LangConstant value) {
        constants.add(value);
        return constIndex(value);
    }

    public int constIndex(LangConstant value) {
        return constants.indexOf(value);
    }

    public void writeToFile(File file) {
        try (var writer = Files.newOutputStream(file.toPath())) {
            writer.write(Util.MAGIC_NUMBER);
            writer.write(Util.toByteArray(imports.size()));
            for (var name : imports) {
                var keyArray = StandardCharsets.UTF_8.encode(name).array();
                var valArray = StandardCharsets.UTF_8.encode(name).array();
                writer.write(Util.toByteArray(keyArray.length));
                writer.write(keyArray);
                writer.write(Util.toByteArray(valArray.length));
                writer.write(valArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(exports.size()));
            for (var export : exports) {
                var byteArray = StandardCharsets.UTF_8.encode(export).array();
                writer.write(Util.toByteArray(byteArray.length));
                writer.write(byteArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(functions.size()));
            for (var entry : functions.entrySet()) {
                var keyArray = StandardCharsets.UTF_8.encode(entry.getKey()).array();
                var valArray = Util.toByteArray(entry.getValue());
                writer.write(Util.toByteArray(keyArray.length));
                writer.write(keyArray);
                writer.write(Util.toByteArray(valArray.length));
                writer.write(valArray);
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error in writing bytecode to file:\n" + e.getMessage());
        }
    }
}
