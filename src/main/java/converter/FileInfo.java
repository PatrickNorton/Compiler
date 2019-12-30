package main.java.converter;

import main.java.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileInfo {
    private Set<String> exports;
    private Set<Pair<String, String>> imports;
    private Map<String, List<Byte>> functions;
    private Set<LangConstant> constants;

    public FileInfo() {
        this.exports = new HashSet<>();
        this.imports = new HashSet<>();
        this.functions = new LinkedHashMap<>();
        this.constants = new LinkedHashSet<>();
    }

    public void addExport(String name) {
        this.exports.add(name);
    }

    public void addImport(String name) {
        this.imports.add(Pair.of(name, name));
    }

    public void addImport(String name, String as) {
        this.imports.add(Pair.of(name, as));
    }

    public void addFunction(String name, List<Byte> bytecode) {
        this.functions.put(name, bytecode);
    }

    public void writeToFile(File file) {
        try (var writer = Files.newOutputStream(file.toPath())) {
            writer.write(Util.MAGIC_NUMBER);
            writer.write(Util.toByteArray(imports.size()));
            for (var pair : imports) {
                var keyArray = StandardCharsets.UTF_8.encode(pair.getKey()).array();
                var valArray = StandardCharsets.UTF_8.encode(pair.getKey()).array();
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
